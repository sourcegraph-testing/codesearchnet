<?php

namespace ZfMetal\EmailCampaigns\Controller;

use Zend\Form\Element\Hidden;
use Zend\Mvc\Controller\AbstractActionController;
use ZfMetal\EmailCampaigns\Entity\DistributionList;
use ZfMetal\EmailCampaigns\Entity\DistributionRecord;

/**
 * DistributionRecordController
 */
class DistributionRecordController extends AbstractActionController
{

    const ENTITY = \ZfMetal\EmailCampaigns\Entity\DistributionRecord::class;
    const SUBSCRIPTION_ACTIVE   = 1;
    const SUBSCRIPTION_INACTIVE = 0;
    /**
     * @var \Doctrine\ORM\EntityManager
     */
    public $em = null;

    /**
     * @var \ZfMetal\Datagrid\Grid
     */
    public $grid = null;

    public function getEm()
    {
        return $this->em;
    }

    public function setEm(\Doctrine\ORM\EntityManager $em)
    {
        $this->em = $em;
    }

    public function getEntityRepository()
    {
        return $this->getEm()->getRepository(self::ENTITY);
    }

    public function getDistributionRecordRepository()
    {
        return $this->getEm()->getRepository(self::ENTITY);
    }

    public function getDistributionListRepository()
    {
        return $this->getEm()->getRepository(DistributionList::class);
    }

    public function __construct(\Doctrine\ORM\EntityManager $em, \ZfMetal\Datagrid\Grid $grid)
    {
        $this->em = $em;
        $this->grid = $grid;
    }

    public function getGrid()
    {
        return $this->grid;
    }

    public function setGrid(\ZfMetal\Datagrid\Grid $grid)
    {
        $this->grid = $grid;
    }

    public function gridAction()
    {
        $id = $this->params('id');
        $distributionList = null;
        if ($id) {
            $distributionList = $this->getDistributionListRepository()->find($id);
            if (!$distributionList) {
                throw new \Exception("Distribution List doesn't exist");
            }

            $this->grid->getCrud()->getSource()->getQb()->andWhere('u.distributionList = :id');
            $this->grid->getCrud()->getSource()->getQb()->setParameter('id', $id);
            $config = $this->grid->getColumnsConfig();
            $config['distributionList']['hidden'] = true;
            $this->grid->setColumnsConfig($config);

            $this->grid->getCrudForm()->remove('distributionList');
            $dth = new Hidden('distributionList');
            $dth->setValue($id);
            $this->grid->getCrudForm()->add($dth);
        }

        $this->grid->addExtraColumn('Desuscribir', '<a class="registroBoton btn btn-primary" title="Desuscribir" href="/email-campaigns/distribution-record/unsubscribe/{{id}}"><span class="glyphicon glyphicon-remove"></span></a> ', 'right');
        $this->grid->prepare();

        return array("grid" => $this->grid, "distributionList" => $distributionList);
    }

    public function unsubscribeAction() {
        $id = $this->params('id');
        if($id){
            /** @var DistributionRecord $distributionRecord */
            $distributionRecord = $this->getEntityRepository()->find($id);
            if (!$distributionRecord) {
                throw new \Exception("Distribution Record not found");
            }
            $distributionRecord->setSubscription(self::SUBSCRIPTION_INACTIVE);
            $distributionListId = $distributionRecord->getDistributionList()->getId();
            $this->getEm()->persist($distributionRecord);
            $this->getEm()->flush();
            $this->flashMessenger()->addSuccessMessage('Se canceló correctamente la suscripción del contacto "' . $distributionRecord->getEmail() . '" en la lista "' . $distributionRecord->getDistributionList()->getNameList() . '".');
        }
        return $this->redirect()->toUrl('/email-campaigns/distribution-record/grid/'.$distributionListId);
    }

}
