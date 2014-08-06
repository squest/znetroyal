(ns znetroyal.dbprocess.maindb
  (:use korma.db)
  (:use korma.core)
  (:require [com.ashafa.clutch :as cl]
            [clojure.string :as st]
            [incanter.core :as in-core]
            [incanter.excel :as in-excel]))


; DATABASE MYSQL

(defdb mysql-znetdb (mysql {:db "znet"
                         :user "root"}))

(defentity t-content
    (database mysql-znetdb)
    (table :content))


(defentity t-video
    (database mysql-znetdb)
    (table :video))


; DATABASE COUCHDB

(def cdb-znet-royalty "znet-royalty")
(def cdb-znet-tutor-users "znet-tutor-users")

; IN MEMORY DATASETS (or something from files)



; FUNCTIONS

(defn store-content [col db]
    (loop [c col]
        (if (empty? c)
            nil
            (do (let [cdata (first c)]
                    (cl/put-document db
                                     (assoc cdata
                                            :tContent true)))
                (recur (rest c))))))

(defn store-video-cc [dbsource dbtarget]
    (loop [n 1]
        (let [raw-data (cl/get-document dbsource (str n))]
            (if (nil? raw-data)
                nil
                (do (let [data (dissoc raw-data :_id :_rev)]
                        (cl/put-document dbtarget
                                         (assoc data
                                                :tVideoCC true)))
                    (recur (inc n)))))))


(defn init-click [data]
      (assoc (dissoc data :played)
             :played {:historic 0}))

(defn init-played [n]
    (let [bulk-data (take n
                          (pmap :value (cl/get-view cdb-znet-royalty
                                              "tVideoCC"
                                              "byVideoID")))]
        (loop [c bulk-data]
            (if (empty? c)
                nil
                (do (cl/put-document cdb-znet-royalty
                                     (init-click (first c)))
                    (recur (rest c)))))))


(defn update-historic [video-id played]
    (let [old-data (->> (cl/get-view cdb-znet-royalty
                                "tVideoCC"
                                "byVideoID"
                                {:key video-id})
                        first
                        :value)
          new-played (if-not (nil? old-data)
                             (+ played (:historic (:played old-data)))
                             played)
          new-data (assoc (dissoc old-data :played)
                          :played {:historic new-played})]
        (if-not (nil? old-data)
                (cl/put-document cdb-znet-royalty
                                 new-data))))



(defn int-cc [content-id]
    (let [data (cl/get-view cdb-znet-royalty
                            "tContent"
                            "byContentID"
                            {:key content-id})
          old-data (:value (first data))
          new-data (assoc (dissoc old-data :id)
                          :contentID (read-string (:id old-data)))]
        (cl/put-document cdb-znet-royalty new-data)))

(defn bulk-int-cc []
    (let [bulk-data (cl/get-view cdb-znet-royalty
                                 "tContent"
                                 "byContentID")]
        (loop [c bulk-data]
            (if (empty? c)
                nil
                (do (let [data (:key (first c))]
                        (int-cc data))
                    (recur (rest c)))))))

(defn normalise-duration [numb value]
    (loop [n 1]
        (if (> n numb)
            "beres"
            (do (let [old-data (:value (first (cl/get-view cdb-znet-royalty
                                                           "tVideoCC"
                                                           "byVideoID"
                                                           {:key n})))]
                    (if-not (nil? old-data)
                            (let [new-data (if (zero? (:duration old-data))
                                               (assoc (dissoc old-data :duration)
                                                      :duration value)
                                               old-data)]
                                 (cl/put-document cdb-znet-royalty
                                                  new-data))))
                (recur (inc n))))))





; FUNCTIONS FOR ROUTINE OPERATIONS

(defn get-videoID [video-field]
    (if (number? video-field)
        (int video-field)
        (let [filename (last (st/split (last (st/split video-field #"/")) #":"))]
             (->> (select t-video
                          (fields :id :filename)
                          (where {:filename filename}))
                  first
                  :id))))

(defn process-excel [filename]
  (filter #(not (nil? %))
    (for [a (second (second (in-excel/read-xls filename)))]
        (let [video-id (get-videoID (get a "video"))]
            (if-not (nil? video-id)
                    {:video-id video-id
                     :click (int (get a "click"))})))))


(defn store-click [video-id played time-frame]
    (let [old-data (:value (first (cl/get-view cdb-znet-royalty
                                               "tVideoCC"
                                               "byVideoID"
                                               {:key video-id})))
          old-played (:played old-data)
          new-played (assoc old-played
                            time-frame played)
          new-data (assoc (dissoc old-data :played)
                          :played new-played)]
        (if-not (nil? old-data)
                (cl/put-document cdb-znet-royalty
                                 new-data))))

(defn bulk-store-click [filename time-frame]
    (let [bulk-data (process-excel filename)]
        (loop [c bulk-data]
            (if (empty? c)
                nil
                (do (let [cdata (first c)]
                        (store-click (:video-id cdata)
                                     (:click cdata)
                                     time-frame))
                    (recur (rest c)))))))

(defn process-all-excel [filename sum-files time-frame]
    (loop [n 1]
        (if (> n sum-files)
            nil
            (do (bulk-store-click
                     (apply str (concat filename (str n) ".xlsx"))
                     time-frame)
                (recur (inc n))))))


(defn count-content-played [content-id time-frame]
    (let [col (cl/get-view cdb-znet-royalty
                           "tVideoCC"
                           "byContentID"
                           {:key content-id})
          jumlah (reduce +' (pmap #(let [data (time-frame (:played (:value %)))]
                                        (if (nil? data)
                                            0
                                            data)) col))
          free? (= "visitor" (:privilege (:value (first
                (cl/get-view cdb-znet-royalty
                             "tContent"
                             "byContentID"
                             {:key content-id})))))]
        jumlah))

(defn update-content-played [content-id]
    (let [data (:value (first (cl/get-view cdb-znet-royalty
                                           "tContent"
                                           "byContentID"
                                           {:key content-id})))
          historic-plays (count-content-played content-id :historic)
          jan-feb14-plays (count-content-played content-id :janfeb14)
          new-data (assoc data
                          :played {:historic historic-plays
                                   :janfeb14 jan-feb14-plays})]
        (if (nil? data)
            nil
            new-data)))

(defn all-content-update [limit]
    (loop [n 1]
        (if (> n limit)
            "beres"
            (do (let [new-data (update-content-played n)]
                    (if (nil? new-data)
                        (println n "yll")
                        (cl/put-document cdb-znet-royalty
                                         new-data)))
                (recur (inc n))))))




(defn count-tutor-played [tutor time-frame]
    (let [col (cl/get-view cdb-znet-royalty
                           "tContent"
                           "byTutor"
                           {:key tutor})]
        (reduce +' (pmap #(count-content-played
                             (:contentID (:value %))
                             time-frame)
                         col))))

(defn content-duration [content-id]
    (let [col (pmap :value
                    (cl/get-view cdb-znet-royalty
                                 "tVideoCC"
                                 "byContentID"
                                 {:key content-id}))]
        (reduce +' (pmap #(let [nums (:duration %)]
                              (if (nil? nums)
                                  0
                                  nums)) col))))

(defn tutor-duration [tutor]
    (let [col (pmap :value (cl/get-view cdb-znet-royalty
                                        "tContent"
                                        "byTutor"
                                        {:key tutor}))]
        (reduce +' (pmap #(content-duration (:contentID %))
                         col))))

(defn total-znet-duration [limit]
    (loop [n 1 res 0]
        (if (> n limit)
            res
            (recur (inc n)
                   (+ res (content-duration n))))))

(defn all-tutors-played [time-frame]
    (let [tutors (pmap :value (cl/get-view cdb-znet-tutor-users
                                           "tTutor"
                                           "byTutor"))]
        (loop [c tutors res nil]
            (if (empty? c)
                res
                (recur (rest c)
                       (conj res {:tutor (:tutor (first c))
                                  :played (count-tutor-played (:tutor (first c))
                                                                time-frame)}))))))

(defn all-tutors-duration []
    (let [tutors (pmap :value (cl/get-view cdb-znet-tutor-users
                                           "tTutor"
                                           "byTutor"))]
        (loop [c tutors res nil]
            (if (empty? c)
                res
                (recur (rest c)
                       (conj res {:tutor (:tutor (first c))
                                  :duration (tutor-duration (:tutor (first c)))}))))))

; SPECIAL STUFFS for calculating top-charts

(defn top-content [time-frame limit top]
    (loop [n 1 res nil]
        (if (> n limit)
            res
            (recur (inc n)
                   (let [cdata (:value (first (cl/get-view cdb-znet-royalty
                                                           "tContent"
                                                           "byContentID"
                                                           {:key n})))]
                       (if (nil? cdata)
                           res
                           (conj res {:played (count-content-played (:contentID cdata)
                                                                    time-frame)
                                      :name (:name cdata)
                                      :tutor (let [tutor (:tutor cdata)]
                                                 (if (nil? tutor)
                                                     "Ga kenal"
                                                     tutor))})))))))

(defn create-top-chart [time-frame title limit top chart-id]
    (let [data (top-content time-frame limit top)]
        (cl/put-document "znet-royalty"
                         {:chartID chart-id
                          :title title
                          :topChart (reverse (sort-by :played data))
                          :trTopChart true})))


(comment
(do
(create-top-chart :historic "Best all time content upto Dec 2013" 4500 200 1)
(create-top-chart :janfeb14 "Top played content Jan-Feb 2014" 4500 200 2)))

; Ad-hoc needs to generate a new thing to database

(defn create-tutor-duration-report [report-id report-title]
    (let [data (all-tutors-duration)]
        (cl/put-document cdb-znet-royalty
                         {:trTutorDuration true
                          :title report-title
                          :reportID report-id
                          :reportContent data})))

(comment
(create-tutor-played-report :historic 1 "Video played july 2012 to dec 2013")
(create-tutor-played-report :janfeb14 2 "Video played jan-feb 2014"))


(defn create-tutor-played-report [time-frame report-id report-title]
    (let [data (all-tutors-played time-frame)]
        (cl/put-document cdb-znet-royalty
                         {:trTutorPlayed true
                          :title report-title
                          :reportID report-id
                          :timeframe time-frame
                          :reportContent data})))




































