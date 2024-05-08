<?php
namespace Devture\Component\DBAL\Repository;

abstract class EavSqlRepository extends BaseSqlRepository {

	const VALUE_TYPE_BOOL = 'b';
	const VALUE_TYPE_INT = 'i';
	const VALUE_TYPE_FLOAT = 'f';
	const VALUE_TYPE_STRING = 's';
	const VALUE_TYPE_NULL = 'n';

	private function getTableNameEav() {
		return $this->getTableName() . '_eav';
	}

	protected function loadModel(array $data) {
		if (!isset($data['_id'])) {
			throw new \InvalidArgumentException('Missing _id field in data.');
		}

		$mainFieldsMap = $this->getMainFieldsMapping();
		foreach ($mainFieldsMap as $fieldKey => $valueType) {
			if (array_key_exists($fieldKey, $data)) {
				$data[$fieldKey] = $this->reverseTransformValue($data[$fieldKey], $valueType);
			}
		}

		//If a special `__eav_data__` attribute is provided, use that, instead of pulling-in from EAV.
		//This is an optimization for the findAllByQuery() case, which aims to avoid one-by-one EAV pulling.
		$eavData = (isset($data['__eav_data__']) ? $data['__eav_data__'] : $this->pullEavData($data['_id']));
		unset($data['__eav_data__']);

		foreach ($eavData as $fieldKey => $fieldValue) {
			$data[$fieldKey] = $fieldValue;
		}

		$data = $this->unflatten($data);

		return $this->createModel($data);
	}

	protected function splitMainAndEavExport(array $export) {
		$exportMain = array();

		//Move whatever is non-eav to $exportMain. Whatever remains would be our eav-export.
		$mainFieldsMap = $this->getMainFieldsMapping();
		foreach ($mainFieldsMap as $fieldKey => $_valueType) {
			if (array_key_exists($fieldKey, $export)) {
				$exportMain[$fieldKey] = $export[$fieldKey];
				unset($export[$fieldKey]);
			} else {
				//Just let this EAV field remain in $export.
			}
		}

		return array($exportMain, /* $exportEav */ $export);
	}

	protected function getMainFieldsMapping() {
		return array(
			'_id' => self::VALUE_TYPE_INT,
		);
	}

	protected function afterEavSaveAndBeforeCommit($entity) {
		//no-op
	}

	public function findBy(array $findBy, array $extra) {
		$where = array();
		$bindParams = array();

		$findByFlattened = $this->flatten($findBy);

		$mainFieldsMapping = $this->getMainFieldsMapping();
		foreach ($findByFlattened as $attribute => $value) {
			if (isset($mainFieldsMapping[$attribute])) {
				$where[] = ' AND ' . $attribute . ' = ?';
				$bindParams[] = $value;
				continue;
			}
		}

		$query = 'SELECT * FROM ' . $this->getTableName() . ' WHERE 1';

		$query .= implode(' ', $where);

		if (isset($extra['sort'])) {
			$orderParts = array();
			$sortByFlattened = $this->flatten($extra['sort']);
			foreach ($sortByFlattened as $attribute => $directionInt) {
				$orderParts[] = $attribute . ' ' . ($directionInt == 1 ? 'ASC' : 'DESC');
			}
			$query .= ' ORDER BY ' . implode(', ', $orderParts);
		}

		if (isset($extra['limit'])) {
			$query .= ' LIMIT ' . (int) $extra['limit'];
		}
		if (isset($extra['skip'])) {
			$query .= ' OFFSET ' . (int) $extra['skip'];
		}

		return $this->findAllByQuery($query, $bindParams);
	}

	/**
	 * {@inheritDoc}
	 * @see \Devture\Component\DBAL\Repository\BaseSqlRepository::findAllByQuery()
	 */
	public function findAllByQuery($query, array $params = array()) {
		//Not using the parent findAllByQuery() implementation, becaus
		//it performs one-by-one EAV fetching, which is very bad for performance.
		//Instead, we find all main records with 1 query. And then fine all EAV entries (for all),
		//with a single additional query (instead of one per main record).

		$records = array();
		$recordIds = array();
		foreach ($this->db->fetchAll($query, $params) as $data) {
			$records[] = $data;
			$recordIds[] = $data['_id'];
		}

		$dataPerNamespaceFlattened = $this->pullEavDataForAll($recordIds);

		$results = array();
		foreach ($records as $data) {
			$data['__eav_data__'] = $dataPerNamespaceFlattened[$data['_id']];
			$results[] = $this->loadModel($data);
		}

		return $results;
	}

	public function add($entity) {
		$this->validateModelClass($entity);

		$hasIdBeforeInsert = ($entity->getId() !== null);

		$export = $this->flatten($this->exportModel($entity));
		list($exportMain, $exportEav) = $this->splitMainAndEavExport($export);

		$exportMain = array_merge($exportMain, ($hasIdBeforeInsert ? array('_id' => $entity->getId()) : array()));

		$exportMain = $this->transformAllUsingMapping($exportMain, $this->getMainFieldsMapping());

		try {
			$this->db->beginTransaction();

			$this->db->insert($this->getTableName(), $exportMain);

			if ($this->db->errorCode() == 0) {
				if (!$hasIdBeforeInsert) {
					//Relying on auto-increment from the database
					$entity->setId((int) $this->db->lastInsertId());
				}

				$this->saveEavData($entity->getId(), $exportEav);

				$this->afterEavSaveAndBeforeCommit($entity);

				$this->db->commit();

				return;
			}
		} catch(\Exception $e){
			$this->db->rollBack();
			throw $e;
		}

		$this->db->rollBack();

		throw new \RuntimeException('Could not perform insert');
	}

	public function update($entity) {
		$this->validateModelClass($entity);
		if ($entity->getId() === null) {
			throw new \LogicException('Cannot update a non-identifiable object.');
		}

		$export = $this->flatten($this->exportModel($entity));
		list($exportMain, $exportEav) = $this->splitMainAndEavExport($export);

		$exportMain = $this->transformAllUsingMapping($exportMain, $this->getMainFieldsMapping());

		try {
			$this->db->beginTransaction();
			if (count($exportMain) > 0) {
				$this->db->update($this->getTableName(), $exportMain, array('_id' => $entity->getId()));
			}
			$this->saveEavData($entity->getId(), $exportEav);

			$this->afterEavSaveAndBeforeCommit($entity);

			$this->db->commit();
		}catch(\Exception $e){
			$this->db->rollBack();
			throw $e;
		}
	}

	public function delete($entity) {
		$this->validateModelClass($entity);
		if ($entity->getId() === null) {
			throw new \LogicException('Cannot delete a non-identifiable object.');
		}
		try {
			$this->db->beginTransaction();
			$this->clearEavData($entity->getId());
			$this->db->delete($this->getTableName(), array('_id' => $entity->getId()));
			$this->db->commit();
			unset($this->models[(string) $entity->getId()]);
			$entity->setId(null);
		}catch(\Exception $e){
			$this->db->rollBack();
			throw $e;
		}
	}

	private function saveEavData($id, array $data) {
		$this->clearEavData($id);

		$rows = array();
		foreach ($data as $attribute => $value) {
			list($valueType, $valueTransformed) = $this->transformValue($value);

			if ($valueTransformed === null) {
				continue;
			}

			$rows[] = array(
				'namespace' => $id,
				'attribute' => $attribute,
				'value' => $valueTransformed,
				'type' => $valueType,
			);
		}

		if (count($rows) === 0) {
			return;
		}

		$keys = array_keys($rows[0]);
		$valuesGroups = array();
		$bindParams = array();
		foreach ($rows as $row) {
			$placeHolders = array_fill(0, count($row), '?');
			$valuesGroups[] = '(' . implode(', ', $placeHolders) . ')';
			$bindParams = array_merge($bindParams, array_values($row));
		}

		$query = '
			INSERT INTO ' . $this->getTableNameEav() . ' (' . implode(', ', $keys) . ')
			VALUES ' . implode(', ', $valuesGroups);

		$this->db->executeUpdate($query, $bindParams, array());
	}

	private function pullEavData($id) {
		return $this->pullEavDataForAll(array($id))[$id];
	}

	private function pullEavDataForAll(array $ids) {
		if (count($ids) === 0) {
			return array();
		}

		$placeholders = array_map(function ($s) { return '?'; }, $ids);

		$query = '
			SELECT *
			FROM ' . $this->getTableNameEav() . '
			WHERE namespace IN (' . implode(', ', $placeholders) . ')
		';

		$dataPerNamespaceFlattened = array();

		//Initialize to make sure that even if the query doesn't provide any EAV rows
		//for a given id, we would still have a result for it, albeit an empty array one.
		foreach ($ids as $id) {
			$dataPerNamespaceFlattened[$id] = array();
		}

		foreach ($this->db->fetchAll($query, $ids) as $row) {
			$dataPerNamespaceFlattened[$row['namespace']][$row['attribute']] = $this->reverseTransformValue($row['value'], $row['type']);
		}

		return $dataPerNamespaceFlattened;
	}

	private function clearEavData($id) {
		$this->db->delete($this->getTableNameEav(), array('namespace' => $id));
	}

	private function flatten(array $data) {
		$dataFlattened = array();

		$makeKeyByPath = function (array $path) {
			return implode('__', $path);
		};

		$work = function (array $dataPartial, array $pathHistory) use (&$dataFlattened, &$work, $makeKeyByPath) {
			foreach ($dataPartial as $attribute => $value) {
				$currentPath = array_merge($pathHistory, array($attribute));

				if (is_array($value)) {
					$work($value, $currentPath);
				} else {
					$dataFlattened[$makeKeyByPath($currentPath)] = $value;
				}
			}
		};

		$work($data, array());

		return $dataFlattened;
	}

	private function unflatten(array $dataFlattened) {
		$data = array();

		foreach ($dataFlattened as $key => $value) {
			//$prefixingKeyParts would be "all but the last one",
			//while $targetKey contains the last one.
			$prefixingKeyParts = explode('__', $key);
			$targetKey = array_pop($prefixingKeyParts);

			$targetRef = &$data;
			foreach ($prefixingKeyParts as $keyPart) {
				if (!isset($targetRef[$keyPart])) {
					$targetRef[$keyPart] = array();
				}
				$targetRef = &$targetRef[$keyPart];
			}

			$targetRef[$targetKey] = $value;
		}

		return $data;
	}

	private function transformValue($value) {
		if ($value === null) {
			return array(self::VALUE_TYPE_NULL, $value);
		}
		if (is_int($value)) {
			return array(self::VALUE_TYPE_INT, $value);
		}
		if (is_float($value)) {
			return array(self::VALUE_TYPE_FLOAT, $value);
		}
		if (is_bool($value)) {
			return array(self::VALUE_TYPE_BOOL, $value ? 1 : 0);
		}
		return array(self::VALUE_TYPE_STRING, $value);
	}

	private function reverseTransformValue($value, $type) {
		if ($value === null) {
			return null;
		}

		switch ($type) {
			case self::VALUE_TYPE_INT:
				return (int) $value;
			case self::VALUE_TYPE_FLOAT:
				return (float) $value;
			case self::VALUE_TYPE_BOOL:
				return ($value == 1 ? true : false);
			case self::VALUE_TYPE_STRING:
				return (string) $value;
			case self::VALUE_TYPE_NULL:
				return null;
			default:
				throw new \InvalidArgumentException('Do not know how to reverse-transform value type: '. $type);
		}
	}

	private function transformAllUsingMapping(array $data, $fieldsToTypesMapping) {
		foreach ($fieldsToTypesMapping as $fieldKey => $valueType) {
			if (isset($data[$fieldKey])) {
				$data[$fieldKey] = $this->transformValue($data[$fieldKey])[1];
			}
		}
		return $data;
	}

}
