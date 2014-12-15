<?php
/**
 * Created by PhpStorm.
 * User: deng
 * Date: 2014/12/13
 * Time: 22:48
 */

namespace Monitor\Model;

use \MongoClient;

class RedisInfoModel
{
    private $mongo;
    private $db;
    private $collection;

    public function __construct()
    {
        $this->mongo = new MongoClient();
        $this->db = $this->mongo->selectDB('redis_info');
    }

    public function init($name)
    {
        $this->collection = $this->db->selectCollection($name);
    }

    public function listByCondition($condition, $limit=5)
    {
        $cursor = $this->collection->find()->limit($limit);
        $result = [];
        foreach($cursor as $document){
            $result[] = $document;
        }
        return $result;
    }
}