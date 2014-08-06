(ns znetroyal.dbprocess.cgroupdb
  (:use korma.db)
  (:use korma.core)
  (:require [com.ashafa.clutch :as cl]
            [clojure.string :as st]
            [znetroyal.dbprocess.maindb :as mdb]))

; Couchdb

(def cdb-znet-royalty "znet-royalty")

; Mysql

(defdb mysql-znetdb (mysql {:db "znet"
                          :user "root"}))

(defentity t-content
    (database mysql-znetdb)
    (table :content))

(defentity t-content-group
    (database mysql-znetdb)
    (table :content-group))

(defentity t-cg-tree-path
    (database mysql-znetdb)
    (table :content-group-tree-path))

; ADHOC



(defn create-level2 []
    (loop [c cg-level2]
        (if (empty? c)
            "beres"
            (do (cl/put-document cdb-znet-royalty
                                 {:tContentGroup true
                                  :cgID (first c)
                                  :cgLevel 2
                                  :parent (:ancestor (first (select t-cg-tree-path
                                                                    (where {:descendant (first c)
                                                                            :length 1}))))
                                  :name (:name
                                           (first
                                              (select t-content-group
                                                      (fields :name)
                                                      (where {:id (first c)}))))})
                (recur (rest c))))))

(defn parenting-content [content-id]
    (let [cg-id (:cg-id (first (select t-content
                                       (where {:id content-id}))))
          data (take 2 (reverse (sort-by :length
                                         (select t-cg-tree-path
                                                 (where {:descendant cg-id})))))
          final-data (hash-map :level1
                               {:cg-id (:ancestor (first data))
                                :name (:name (first (select t-content-group
                                                            (where {:id (:ancestor (first data))}))))}
                               :level2
                               {:cg-id (:ancestor (second data))
                                :name (:name (first (select t-content-group
                                                            (where {:id (:ancestor (second data))}))))})]
        final-data))


(defn bulk-parenting [limit]
    (let [coldata (pmap :value
                        (cl/get-view cdb-znet-royalty
                               "tContent"
                               "byContentID"))]
        (loop [c coldata n 1]
            (if (or (> n limit) (empty? c))
                "beres"
                (do (let [cdata (first c)
                          content-id (:contentID cdata)
                          temp-parents (parenting-content content-id)
                          final-data (assoc cdata
                                         :parentLevel1 (:level1 temp-parents)
                                         :parentLevel2 (:level2 temp-parents))]
                        (cl/put-document cdb-znet-royalty
                                         final-data))
                    (recur (rest c)
                           (inc n)))))))


(defn revise-parents [limit]
    (let [raw-data (pmap :value
                         (cl/get-view cdb-znet-royalty
                                      "tContent"
                                      "byContentID"))]
        (loop [c raw-data n 1]
          (if (or (empty? c)
                  (> n limit))
              "beres"
              (do (let [cdata (first c)
                        new-data (assoc (dissoc cdata
                                                :parentLevel1
                                                :parentLevel2)
                                        :parentIdLevel1 (:cg-id (:parentLevel1 cdata))
                                        :parentIdLevel2 (:cg-id (:parentLevel2 cdata)))]
                      (cl/put-document cdb-znet-royalty new-data))
                  (recur (rest c)
                       (inc n)))))))


(defn count-cg1-played [cg-id time-frame]
    (let [data (pmap :value (cl/get-view cdb-znet-royalty
                                         "tContent"
                                         "byParentIdLevel1"
                                         {:key cg-id}))]
        (reduce +' (pmap #(time-frame (:played %))
                         data))))



(defn count-cg2-played [cg-id time-frame]
    (let [data (pmap :value (cl/get-view cdb-znet-royalty
                                         "tContent"
                                         "byParentIdLevel2"
                                         {:key cg-id}))]
        (reduce +' (pmap #(time-frame (:played %))
                         data))))

(defn store-level1-played [limit]
    (let [data (pmap :value (cl/get-view cdb-znet-royalty
                                         "tContentGroup"
                                         "byCgID"
                                         {:cgLevel 1}))]
        (loop [n 1 c data]
            (if (or (empty? c)
                    (> n limit))
                "beres"
                (do (let [cdata (first c)
                          played-historic (count-cg1-played (:cgID cdata) :historic)
                          played-janfeb14 (count-cg1-played (:cgID cdata) :janfeb14)
                          new-data (assoc cdata
                                         :played
                                         {:historic played-historic
                                          :janfeb14 played-janfeb14})]
                        (cl/put-document cdb-znet-royalty new-data))
                    (recur (inc n)
                           (rest c)))))))

(defn store-level2-played [limit]
    (let [data (filter #(= 2 (:cgLevel %))
                       (pmap :value (cl/get-view cdb-znet-royalty
                                                 "tContentGroup"
                                                 "byCgID")))]
        (loop [n 1 c data]
            (if (or (empty? c)
                    (> n limit))
                "beres"
                (do (let [cdata (first c)
                          played-historic (count-cg2-played (:cgID cdata) :historic)
                          played-janfeb14 (count-cg2-played (:cgID cdata) :janfeb14)
                          new-data (assoc cdata
                                         :played
                                         {:historic played-historic
                                          :janfeb14 played-janfeb14})]
                        (cl/put-document cdb-znet-royalty new-data))
                    (recur (inc n)
                           (rest c)))))))

(comment
    (store-level1-played 6)
    (store-level2-played 100))












































