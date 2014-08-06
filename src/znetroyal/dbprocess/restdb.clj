(ns znetroyal.dbprocess.restdb
  (:use korma.db)
  (:use korma.core)
  (:require [com.ashafa.clutch :as cl]
            [clojure.string :as st]
            [znetroyal.dbprocess.maindb :as mdb]))

(def cdb-znet-royalty "znet-royalty")

(defn get-links []
    (hash-map :linksLevel1 ["Video played up-to Dec 2013"
                           "Video played Jan-Feb 2014"]
              :linksLevel2 ["Video played up-to Dec 2013"
                           "Video played Jan-Feb 2014"]))

(defn get-init-report []
    (let [raw-data (filter #(= 1 (:cgLevel %))
                           (pmap :value
                                 (cl/get-view cdb-znet-royalty
                                              "tContentGroup"
                                              "byCgID")))
          sorted-data (reverse (sort-by #(:historic (:played %))
                                        raw-data))
          final-data (for [a sorted-data]
                          (hash-map :rank (inc (.indexOf sorted-data a))
                                    :name (:name a)
                                    :played (:historic (:played a))
                                    :link (apply str
                                                 (concat "/topchartcategories?cgLevel="
                                                         "1"
                                                         "&cgID="
                                                         (str (:cgID a))
                                                         "&timeframe="
                                                         ":historic"))))]
        (hash-map :title "Best performing Level-1 up to Dec 2013"
                  :report final-data)))

(defn get-report [cat-level time-frame]
    (let [raw-data (filter #(= cat-level (:cgLevel %))
                           (pmap :value
                                 (cl/get-view cdb-znet-royalty
                                              "tContentGroup"
                                              "byCgID")))
          sorted-data (reverse (sort-by #(time-frame (:played %))
                                        raw-data))
          final-data (for [a sorted-data]
                          (hash-map :rank (inc (.indexOf sorted-data a))
                                    :name (:name a)
                                    :played (time-frame (:played a))
                                    :link (apply str
                                                 (concat "/topchartcategories?cgLevel="
                                                         (str cat-level)
                                                         "&cgID="
                                                         (str (:cgID a))
                                                         "&timeframe="
                                                         (str time-frame)))))]
        (hash-map :title (apply str (concat "Best performing Level"
                                            (str cat-level)
                                            " "
                                            (cond (= :historic time-frame)
                                                  "upto Dec 2013"
                                                  (= :janfeb14 time-frame)
                                                  "Jan-Feb 2014"
                                                  :else "Jojon")))
                  :report final-data)))




(defn get-data [requested-data]
    (cond (= "links" requested-data)
          (get-links)
          (= "initReport" requested-data)
          (get-init-report)
          :else nil))

















