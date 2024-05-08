<?php

namespace Interpro\QuickStorage\Laravel;

use Illuminate\Container\Container as App;
use Illuminate\Support\Facades\Log;
use Interpro\QuickStorage\Concept\Exception\BuildQueryException;
use Interpro\QuickStorage\Concept\Param\GroupParam;
use Interpro\QuickStorage\Concept\QSource as QSourceInterface;
use Interpro\QuickStorage\Concept\Sorting\GroupSortingSet;
use Interpro\QuickStorage\Concept\Specification\GroupSpecificationSet;
use Interpro\QuickStorage\Concept\StorageStructure as StorageStructureInterface;
use Illuminate\Support\Facades\DB;

class QSource implements QSourceInterface
{
    private $storageStruct;
    private $app;
    private $groupSortingSet;
    private $groupSpecificationSet;
    private $groupParam;

    public function __construct(StorageStructureInterface $storageStruct, App $app, GroupSortingSet $groupSortingSet, GroupSpecificationSet $groupSpecificationSet, GroupParam $groupParam)
    {
        $this->app           = $app;
        $this->storageStruct = $storageStruct;
        $this->groupSortingSet = $groupSortingSet;
        $this->groupSpecificationSet = $groupSpecificationSet;
        $this->groupParam = $groupParam;
    }

    private function select_field_for_block($query, $block_name, $field_name)
    {
        $model = $this->app->make($this->storageStruct->getModelNameByFieldBlock($block_name, $field_name));

        $table_q = $model->selectRaw('block_name as '.$field_name.'_block, value as '.$field_name)
            ->whereRaw('name = "'.$field_name.'"')
            ->whereRaw('group_id = 0');

        $query->leftJoin(DB::raw('('.$table_q->toSql().') AS '.$field_name.'_table'), function($join) use ($field_name)
        {
            $join->on('blocks.name', '=', $field_name.'_table.'.$field_name.'_block');
        });
    }

    private function select_field_for_group($query, $block_name, $group_name, $field_name)
    {
        $model = $this->app->make($this->storageStruct->getModelNameByFieldGroup($block_name, $group_name, $field_name));

        $table_q = $model->selectRaw('group_id as '.$field_name.'_group_id, value as '.$field_name)
            ->whereRaw('name = "'.$field_name.'"');

        $query->leftJoin(DB::raw('('.$table_q->toSql().') AS '.$field_name.'_table'), function($join) use ($field_name)
        {
            $join->on('groups.id', '=', $field_name.'_table.'.$field_name.'_group_id');
        });
    }

    /**
     * @param string $block_name
     *
     * @return array
     */
    public function blockQuery($block_name)
    {
        $model_block = $this->app->make('Interpro\QuickStorage\Laravel\Model\Block');

        $block_q = $model_block->query();

        $block_q->where('name', '=', $block_name);

        $fields = $this->storageStruct->getBlockFieldsFlat($block_name);

        $fields_except = ['name', 'title', 'show'];

        foreach($fields as $field_name)
        {
            if(!in_array($field_name, $fields_except))
            {
                $this->select_field_for_block($block_q, $block_name, $field_name);
            }
        }

        $collection = $block_q->get($fields);

        if(!$collection->isEmpty())
        {
            $block_obj = $collection->first();

            return $block_obj->toArray();

        }else{

            throw new BuildQueryException('Блок '.$block_name.' не найден в базе данных.');
        }
    }

    /**
     * @param string $block_name
     * @param string $group_name
     *
     * @return array
     */
    public function groupQuery($block_name, $group_name)
    {
        $model_group = $this->app->make('Interpro\QuickStorage\Laravel\Model\Group');

        $group_q = $model_group->query();

        $group_q->where('group_name', '=', $group_name);

        $fields = $this->storageStruct->getGroupFieldsFlat($block_name, $group_name);

        $fields_except = ['id', 'owner_id', 'group_name', 'group_owner_name', 'block_name', 'title', 'slug', 'sorter', 'show'];

        foreach($fields as $field_name)
        {
            if(!in_array($field_name, $fields_except))
            {
                $this->select_field_for_group($group_q, $block_name, $group_name, $field_name);
            }
        }

        //Условия
        $this->groupSpecificationSet->setCurrentGroup($group_name);
        foreach($this->groupSpecificationSet as $spec)
        {
            $spec->asScope($group_q);
        }

        //Сортировка
        $this->groupSortingSet->setCurrentGroup($group_name);
        foreach($this->groupSortingSet as $spec)
        {
            $spec->apply($group_q);
        }

        //Применить параметры
        $this->groupParam->apply($group_name, $group_q);



        return $group_q->get($fields)->toArray();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     *
     * @return array
     */
    public function groupCount($block_name, $group_name)
    {
        $model_group = $this->app->make('Interpro\QuickStorage\Laravel\Model\Group');

        $group_q = $model_group->query();

        $group_q->where('group_name', '=', $group_name);

        $fields = $this->storageStruct->getGroupFieldsFlat($block_name, $group_name);

        //Условия могут быть на любое из полей, поэтому подсоединим все
        $fields_except = ['id', 'owner_id', 'group_name', 'group_owner_name', 'block_name', 'title', 'slug', 'sorter', 'show'];

        foreach($fields as $field_name)
        {
            if(!in_array($field_name, $fields_except))
            {
                $this->select_field_for_group($group_q, $block_name, $group_name, $field_name);
            }
        }

        //Условия
        $this->groupSpecificationSet->setCurrentGroup($group_name);
        foreach($this->groupSpecificationSet as $spec)
        {
            $spec->asScope($group_q);
        }

        return $group_q->count();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param int $group_id
     *
     * @return array
     */
    public function groupItemQuery($block_name, $group_name, $group_id)
    {
        $model_group = $this->app->make('Interpro\QuickStorage\Laravel\Model\Group');

        $group_q = $model_group->query();

        $group_q->where('block_name', '=', $block_name);
        $group_q->where('group_name', '=', $group_name);
        $group_q->where('id', '=', $group_id);

        $fields = $this->storageStruct->getGroupFieldsFlat($block_name, $group_name);

        $fields_except = ['id', 'owner_id', 'group_name', 'group_owner_name', 'block_name', 'title', 'slug', 'sorter', 'show'];

        foreach($fields as $field_name)
        {
            if(!in_array($field_name, $fields_except))
            {
                $this->select_field_for_group($group_q, $block_name, $group_name, $field_name);
            }
        }

        $collection = $group_q->get($fields);

        if(!$collection->isEmpty())
        {
            $item_obj = $collection->first();

            return $item_obj->toArray();

        }else{

            throw new BuildQueryException('Данные элемента группы ('.$group_name.'-'.$group_id.') не найдены в базе.');
        }
    }


    /**
     * @param string $block_name
     * @param string $group_name
     * @param string $slug
     *
     * @return array
     */
    public function groupItemBySlugQuery($block_name, $group_name, $slug)
    {
        $model_group = $this->app->make('Interpro\QuickStorage\Laravel\Model\Group');

        $group_q = $model_group->query();

        $group_q->where('block_name', '=', $block_name);
        $group_q->where('group_name', '=', $group_name);
        $group_q->where('slug', '=', $slug);

        $fields = $this->storageStruct->getGroupFieldsFlat($block_name, $group_name);

        $fields_except = ['id', 'owner_id', 'group_name', 'group_owner_name', 'block_name', 'title', 'slug', 'sorter', 'show'];

        foreach($fields as $field_name)
        {
            if(!in_array($field_name, $fields_except))
            {
                $this->select_field_for_group($group_q, $block_name, $group_name, $field_name);
            }
        }

        $collection = $group_q->get($fields);

        if(!$collection->isEmpty())
        {
            $item_obj = $collection->first();

            return $item_obj->toArray();

        }else{

            throw new BuildQueryException('Данные элемента группы по слагу ('.$group_name.'-'.$slug.') не найдены в базе.');
        }
    }


    /**
     * @param string $block_name
     * @param string $image_name
     *
     * @return array
     */
    public function oneImageQueryForBlock($block_name, $image_name)
    {
        $model_image = $this->app->make('Interpro\QuickStorage\Laravel\Model\Imageitem');

        $image_q = $model_image->query();

        $image_q->where('block_name', '=', $block_name);
        $image_q->where('name', '=', $image_name);

        $collection = $image_q->get();

        if(!$collection->isEmpty())
        {
            $block_obj = $collection->first();

            return $block_obj->toArray();

        }else{

            throw new BuildQueryException('Данные картинки '.$image_name.' блока '.$block_name.' не найдены в базе.');
        }
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param int $group_id
     * @param string $image_name
     *
     * @return array
     */
    public function oneImageQueryForGroup($block_name, $group_name, $group_id, $image_name='')
    {
        $model_image = $this->app->make('Interpro\QuickStorage\Laravel\Model\Imageitem');

        $image_q = $model_image->query();

        $image_q->where('block_name', '=', $block_name);
        $image_q->where('group_name', '=', $group_name);
        if($image_name){$image_q->where('name', '=', $image_name);}
        $image_q->where('group_id', '=', $group_id);

        return $image_q->get()->toArray();
    }

    /**
     * @param string $block_name
     *
     * @return array
     */
    public function imageQueryForBlock($block_name)
    {
        $model_image = $this->app->make('Interpro\QuickStorage\Laravel\Model\Imageitem');

        $image_q = $model_image->query();

        $image_q->where('block_name', '=', $block_name);
        $image_q->where('group_id', '=', 0);

        return $image_q->get()->toArray();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     *
     * @return array
     */
    public function imageQueryForGroup($block_name, $group_name)
    {
        $model_image = $this->app->make('Interpro\QuickStorage\Laravel\Model\Imageitem');

        $image_q = $model_image->query();

        $image_q->where('block_name', '=', $block_name);
        $image_q->where('group_name', '=', $group_name);

        return $image_q->get()->toArray();
    }




    //Запросы для кропов картинок
    /**
     * @param string $block_name
     * @param string $image_name
     *
     * @return array
     */
    public function cropQueryForBlock($block_name, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}

        return $crop_q->get()->toArray();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param string $image_name
     *
     * @return array
     */
    public function cropQueryForGroup($block_name, $group_name, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        $crop_q->where('group_name', '=', $group_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}

        return $crop_q->get()->toArray();
    }

    /**
     * @param string $block_name
     * @param string $image_name
     * @param string $crop_name
     *
     * @return array
     */
    public function cropQueryForBlockForCrop($block_name, $crop_name, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}
        $crop_q->where('name', '=', $crop_name);

        $collection = $crop_q->get();

        if(!$collection->isEmpty())
        {
            $_obj = $collection->first();

            return $_obj->toArray();

        }else{

            throw new BuildQueryException('Данные кропа '.$crop_name.' блока '.$block_name.' картинки '.$image_name.' не найдены в базе.');
        }
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param string $image_name
     *
     * @return array
     */
    public function cropQueryForGroupForCrop($block_name, $group_name, $crop_name, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        $crop_q->where('group_name', '=', $group_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}
        $crop_q->where('name', '=', $crop_name);

        return $crop_q->get()->toArray();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param int $group_id
     * @param string $image_name
     * @param string $crop_name
     *
     * @return array
     */
    public function oneCropQueryForGroup($block_name, $group_name, $group_id, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        $crop_q->where('group_name', '=', $group_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}
        $crop_q->where('group_id',   '=', $group_id);

        return $crop_q->get()->toArray();
    }

    /**
     * @param string $block_name
     * @param string $group_name
     * @param int $group_id
     * @param string $image_name
     * @param string $crop_name
     *
     * @return array
     */
    public function oneCropQueryForGroupForCrop($block_name, $group_name, $group_id, $crop_name, $image_name='')
    {
        $model_crop = $this->app->make('Interpro\QuickStorage\Laravel\Model\Cropitem');

        $crop_q = $model_crop->query();

        $crop_q->where('block_name', '=', $block_name);
        $crop_q->where('group_name', '=', $group_name);
        if($image_name){$crop_q->where('image_name', '=', $image_name);}
        $crop_q->where('name',       '=', $crop_name);
        $crop_q->where('group_id',   '=', $group_id);

        $collection = $crop_q->get();

        if(!$collection->isEmpty())
        {
            $_obj = $collection->first();

            return $_obj->toArray();

        }else{

            throw new BuildQueryException('Данные кропа '.$crop_name.' блока '.$block_name.' группы '.$group_name.' картинки '.$image_name.' не найдены в базе.');
        }
    }

}
