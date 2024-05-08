<?php
/**
 * Link         :   http://www.phpcorner.net
 * User         :   qingbing<780042175@qq.com>
 * Date         :   2018-12-12
 * Version      :   1.0
 */

namespace DbSupports\Builder\Traits;


use DbSupports\Builder\Criteria;

trait BuilderFind
{
    /**
     * 设置 SELECT-distinct 子句
     * @param bool $isDistinct
     * @return $this
     */
    public function setDistinct($isDistinct)
    {
        if (true === $isDistinct) {
            $this->query['distinct'] = true;
        } else {
            $this->query['distinct'] = false;
        }
        return $this;
    }

    /**
     * 设置 SELECT-select 子句
     * @param mixed $select
     * @return $this
     */
    public function setSelect($select)
    {
        unset($this->query['select']);
        return $this->addSelect($select);
    }

    /**
     * 添加 SELECT-select 内容
     * @param mixed $select
     * @return $this
     */
    public function addSelect($select)
    {
        if (null === $select) {
            return $this;
        }
        if (is_array($select)) {
            $t = [];
            foreach ($select as $field => $alias) {
                if (is_int($field)) {
                    $alias = $this->quoteColumnName($alias);
                    array_push($t, "{$alias}");
                } else {
                    $field = $this->quoteColumnName($field);
                    $alias = $this->quoteColumnName($alias);
                    array_push($t, "{$field} AS {$alias}");
                }
            }
            $select = implode(',', $t);
        }
        if (isset($this->query['select']) && !empty($this->query['select'])) {
            $select = "{$this->query['select']},{$select}";
        }
        $this->query['select'] = $select;
        return $this;
    }

    /**
     * 设置 SQL 语句主表别名
     * @param string $alias
     * @return $this
     */
    public function setAlias($alias = null)
    {
        if (!empty($alias)) {
            $this->query['alias'] = $alias;
        } else {
            unset($this->query['alias']);
        }
        return $this;
    }

    /**
     * 设置 SELECT-join 子句
     * @param string $join
     * @return $this
     */
    public function setJoin($join)
    {
        unset($this->query['join']);
        return $this->addJoin($join);
    }

    /**
     * 添加 SELECT-join 内容
     * @param string $join
     * @return $this
     */
    public function addJoin($join)
    {
        if (null === $join)
            return $this;
        if (!isset($this->query['join'])) {
            $this->query['join'] = [];
        }
        $this->query['join'][] = $join;
        return $this;
    }

    /**
     * 设置 SELECT-group 子句
     * @param string $group
     * @return $this
     */
    public function setGroup($group)
    {
        if (!empty($group)) {
            $this->query['group'] = $group;
        } else {
            unset($this->query['group']);
        }
        return $this;
    }

    /**
     * 设置 SELECT-having 子句
     * @param string $having
     * @return $this
     */
    public function setHaving($having)
    {
        if (!empty($having)) {
            $this->query['having'] = $having;
        } else {
            unset($this->query['having']);
        }
        return $this;
    }

    /**
     * 设置 SELECT-union 子句
     * @param string $union
     * @return $this
     */
    public function setUnion($union)
    {
        unset($this->query['union']);
        return $this->addUnion($union);
    }

    /**
     * 添加 SELECT-union 内容
     * @param string $union
     * @return $this
     */
    public function addUnion($union)
    {
        if (null === $union)
            return $this;
        if (!isset($this->query['union'])) {
            $this->query['union'] = [];
        }
        $this->query['union'][] = $union;
        return $this;
    }

    /**
     * 设置 SELECT-order 子句
     * @param string $order
     * @return $this
     */
    public function setOrder($order)
    {
        if (!empty($order)) {
            $this->query['order'] = $order;
        } else {
            unset($this->query['order']);
        }
        return $this;
    }

    /**
     * 设置 SELECT-limit 子句
     * @param int $limit
     * @return $this
     */
    public function setLimit($limit)
    {
        if ($limit >= 0) {
            $this->query['limit'] = $limit;
        } else {
            unset($this->query['limit']);
        }
        return $this;
    }

    /**
     * 设置 SELECT-offset 子句
     * @param int $offset
     * @return $this
     */
    public function setOffset($offset)
    {
        if ($offset > 0) {
            $this->query['offset'] = $offset;
        } else {
            unset($this->query['offset']);
        }
        return $this;
    }

    /**
     * 添加SQL标准
     * @param Criteria $criteria
     * @param string $operator
     * @return $this
     */
    public function addCriteria($criteria, $operator = 'AND')
    {
        if (!$criteria instanceof Criteria) {
            return $this;
        }
        $query = $criteria->getQuery();
        // distinct
        isset($query['distinct']) && $this->setDistinct($query['distinct']);
        // select
        isset($query['select']) && !empty($query['select']) && $this->addSelect($query['select']);
        // table
        isset($query['table']) && !empty($query['table']) && $this->setTable($query['table']);
        // alias
        isset($query['alias']) && !empty($query['alias']) && $this->setAlias($query['alias']);
        // join
        if (isset($query['join']) && !empty($query['join'])) {
            foreach ($query['join'] as $join) {
                $this->addJoin($join);
            }
        }
        // where
        // 添加 WHERE 条件
        isset($query['where']) && $this->addWhere($query['where'], [], $operator);
        // group
        isset($query['group']) && !empty($query['group']) && $this->setGroup($query['group']);
        // having
        isset($query['having']) && !empty($query['having']) && $this->setHaving($query['having']);
        // union
        if (isset($query['union']) && !empty($query['union'])) {
            foreach ($query['union'] as $union) {
                $this->addUnion($union);
            }
        }
        // order
        isset($query['order']) && !empty($query['order']) && $this->setOrder($query['order']);
        // limit
        isset($query['limit']) && $this->setLimit($query['limit']);
        // offset
        isset($query['offset']) && $this->setOffset($query['offset']);
        // bind params
        $this->addParams($criteria->getParams());

        return $this;
    }
}