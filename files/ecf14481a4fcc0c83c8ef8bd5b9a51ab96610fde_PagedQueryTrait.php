<?php

/*
 * This file is part of the Doctrine DBAL Util package.
 *
 * (c) Jean-Bernard Addor
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

namespace DoctrineDbalUtil\Connection\Pagerfanta;

use Doctrine\DBAL\Query\QueryBuilder;
use DoctrineDbalUtil\Connection\ConnectionAbstractTrait;
use Pagerfanta\Adapter\DoctrineDbalAdapter;
use Pagerfanta\Pagerfanta;
use PagerfantaAdapters\Doctrine\DBAL\DoctrineDbal2ModifiersAdapter;

trait PagedQueryTrait
{
    use ConnectionAbstractTrait;

    public function getManyToManyWherePager($base_table, $base_id,
        $link_base_id, $link_table, $link_distant_id,
        $distant_id, $distant_table, array $where, $orderby = '')
    // PageFanta dependency should be isolated!
    {
        $queryBuilder = $this->getManyToManyWhereQueryBuilder($base_table, $base_id, $link_base_id, $link_table, $link_distant_id, $distant_id, $distant_table, $where, $orderby);
        // $unorderedQueryBuilder = $this->getManyToManyWhereQueryBuilder($base_table, $base_id, $link_base_id, $link_table, $link_distant_id, $distant_id, $distant_table, $where);
        // TODO: do not make it twice...

        $finishQueryBuilderModifier = function (QueryBuilder $queryBuilder) use ($orderby) {
            if ('' !== $orderby):
                $queryBuilder->orderBy($orderby, 'ASC');
            endif;
        };

        $countQueryBuilderModifier = function (QueryBuilder $queryBuilder) {
            $queryBuilder->select('COUNT(DISTINCT base.uuid) AS total_results') // ->orderBy(null) does not remove orderby
                // ->groupBy('base.term') // suggested by Postgres error // TODO: Would it be only needed for counting?
                ->setMaxResults(1);
        };

        $adapter = new DoctrineDbal2ModifiersAdapter($queryBuilder, $finishQueryBuilderModifier, $countQueryBuilderModifier);

        return new Pagerfanta($adapter);
    }

    public function getWhereManyToManyToManyPager($base_table, $base_id, $base_link_base_id, $base_link_table, $base_link_distant_id, $distant_link_base_id, $distant_link_table, $distant_link_distant_id, $distant_id, $distant_table, array $where)
    // PageFanta dependency should be isolated!
    {
        $queryBuilder = $this->getWhereManyToManyToManyQueryBuilder($base_table, $base_id, $base_link_base_id, $base_link_table, $base_link_distant_id, $distant_link_base_id, $distant_link_table, $distant_link_distant_id, $distant_id, $distant_table, $where);

        $countQueryBuilderModifier = function (QueryBuilder $queryBuilder) {
            $queryBuilder->select('COUNT(DISTINCT distant.uuid) AS total_results')
                  ->setMaxResults(1);
        };

        $adapter = new DoctrineDbalAdapter($queryBuilder, $countQueryBuilderModifier);

        return new Pagerfanta($adapter);
    }

    public function getMoreManyToManyWherePager($more_table, $more_id, $base_more, $base_table, $base_id, $link_base_id, $link_table, $link_distant_id, $distant_id, $distant_table, array $where)
    // PageFanta dependency should be isolated!
    {
        $queryBuilder = $this->getMoreManyToManyWhereQueryBuilder(
            $more_table, $more_id, $base_more, $base_table, $base_id,
            $link_base_id, $link_table, $link_distant_id, $distant_id,
            $distant_table, $where);

        $countQueryBuilderModifier = function (QueryBuilder $queryBuilder) {
            $queryBuilder->select('COUNT(DISTINCT base.uuid) AS total_results')
                  ->setMaxResults(1);
        };

        $adapter = new DoctrineDbalAdapter($queryBuilder, $countQueryBuilderModifier);

        return new Pagerfanta($adapter);
    }

    public function getUrlIndexPager($more_table, $more_id, $base_more, $base_table, $base_id, $link_base_id, $link_table, $link_distant_id, $distant_id, $distant_table, array $where)
    // PageFanta dependency should be isolated!
    {
        $queryBuilder = $this->getUrlIndexQueryBuilder(
            $more_table, $more_id, $base_more, $base_table, $base_id,
            $link_base_id, $link_table, $link_distant_id, $distant_id,
            $distant_table, $where);

        $finishQueryBuilderModifier = function (QueryBuilder $queryBuilder) {
            $queryBuilder
                ->groupBy('more.uuid') // suggested by Postgres error // TODO: Would it be only needed for counting?
                ->addGroupBy('base.uuid') // suggested by Postgres error // TODO: Would it be only needed for counting?
                ->orderBy('count(base.uuid=taxo.owned_url_uuid)', 'ASC')
            ;
        };

        $countQueryBuilderModifier = function (QueryBuilder $queryBuilder) { // TODO: a simplified query may improve performance!
            // $queryBuilder->select('COUNT(DISTINCT base.uuid) AS total_results, count(base.uuid=taxo.owned_url_uuid) AS taxocount')
            $queryBuilder->select('COUNT(DISTINCT base.uuid) AS total_results')
                  ->setMaxResults(1);
        };

        $adapter = new DoctrineDbal2ModifiersAdapter($queryBuilder, $finishQueryBuilderModifier, $countQueryBuilderModifier);

        return new Pagerfanta($adapter);
    }
}
