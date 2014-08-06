(ns znetroyal.dbprocess.reportdb
  (:require [com.ashafa.clutch :as cl]
            [clojure.string :as st]
            [znetroyal.dbprocess.maindb :as db]))

; Couchdb databases

; Main database for royalty calculation & other reporting
(def cdb-znet-royalty "znet-royalty")

; Core cross-purpose database to access zenius.net tutors and users
(def cdb-znet-tutor-users "znet-tutor-users")

; functions for top-chart categories

(defn topchart-categories [cg-level cg-id time-frame]
    (let [raw-data (pmap :value
                         (cl/get-view cdb-znet-royalty
                                         "tContent"
                                         (if (= 1 cg-level)
                                             "byParentIdLevel1"
                                             "byParentIdLevel2")
                                         {:key cg-id}))
          sorted-data (take 50
                            (reverse (sort-by #(time-frame (:played %))
                                              raw-data)))
          final-data (for [a sorted-data]
                          (hash-map :rank (inc (.indexOf sorted-data a))
                                    :played (time-frame (:played a))
                                    :name (:name a)))]
        (hash-map :title (apply str
                                (concat "Best performing content in content-group "
                                        (str cg-id)))
                  :topchart final-data)))

; functions to serve topchart data reporting

(defn links-topchart []
    (let [data (cl/get-view cdb-znet-royalty
                            "trTopChart"
                            "linkByChartID")
          final-data (pmap #(hash-map
                              :url (apply str
                                       (concat "/topchart?linkID1="
                                               (str (:key %))
                                               "&linkID2=1"))
                              :title (:value %))
                           data)]
        final-data))

(defn options-topchart [chartID]
    (vector
     {:url (apply str (concat "/topchart?linkID1="
                              chartID
                              "&linkID2=1"))
      :text "Top 20"}
     {:url (apply str (concat "/topchart?linkID1="
                              chartID
                              "&linkID2=2"))
      :text "Top 50"}
     {:url (apply str (concat "/topchart?linkID1="
                              chartID
                              "&linkID2=3"))
      :text "Top 100"}
     {:url (apply str (concat "/topchart?linkID1="
                              chartID
                              "&linkID2=4"))
      :text "Top 200"}))


(defn init-topchart-data []
    (let [links (links-topchart)
          options (options-topchart "1")
          raw-data (:value
                      (first
                         (cl/get-view cdb-znet-royalty
                                      "trTopChart"
                                      "byChartID"
                                      {:key 1})))
          title (:title raw-data)
          data (take 20 (:topChart raw-data))
          real-data (reverse
                       (sort-by :played data))
          final-data (pmap #(assoc %
                                   :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :links links
         :options options
         :topchart final-data}))


(defn topchart-data [link-id1 link-id2]
    (let [links (links-topchart)
          options (options-topchart link-id1)
          raw-data (:value
                      (first
                         (cl/get-view cdb-znet-royalty
                                      "trTopChart"
                                      "byChartID"
                                      {:key (read-string link-id1)})))
          title (:title raw-data)
          taken (cond (= "1" link-id2) 20
                      (= "2" link-id2) 50
                      (= "3" link-id2) 100
                      :else 200)
          data (take taken (:topChart raw-data))
          real-data (reverse
                       (sort-by :played data))
          final-data (pmap #(assoc %
                                   :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :links links
         :options options
         :topchart final-data}))

; functions to serve video played reporting data

(defn links-royalty []
    (let [data (cl/get-view cdb-znet-royalty
                            "trTutorPlayed"
                            "linkByReportID")
          final-data (pmap #(hash-map
                              :url (apply str
                                       (concat "/royalty?linkID="
                                               (str (:key %))))
                              :title (:value %))
                           data)]
        final-data))




(defn init-royalty-data []
    (let [raw-data (:value
                      (first
                         (cl/get-view cdb-znet-royalty
                                      "trTutorPlayed"
                                      "byReportID"
                                      {:key 1})))
          title (:title raw-data)
          links (links-royalty)
          data (:reportContent raw-data)
          real-data (reverse
                       (sort-by :played data))
          final-data (pmap #(assoc %
                                   :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :links links
         :tutorPlayed final-data}))

(defn royalty-data [link-id]
    (let [raw-data (:value
                      (first
                         (cl/get-view cdb-znet-royalty
                                      "trTutorPlayed"
                                      "byReportID"
                                      {:key (read-string link-id)})))
          title (:title raw-data)
          links (links-royalty)
          data (:reportContent raw-data)
          real-data (reverse
                       (sort-by :played data))
          final-data (pmap #(assoc %
                                   :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :links links
         :tutorPlayed final-data}))


; functions to server duration page reporting

(defn links-duration []
    (let [data (pmap :value (cl/get-view cdb-znet-royalty
                                         "trTutorDuration"
                                         "byReportID"))]
        (loop [cdata data res []]
            (if (empty? cdata)
                (sort-by :url res)
                (recur (rest cdata)
                       (conj res
                             {:title (:title (first cdata))
                              :url (apply str
                                          (concat "/duration?linkID="
                                                  (str (:reportID (first cdata)))))}))))))

(defn init-duration-data []
    (let [raw-data (:value
                      (first
                         (cl/get-view cdb-znet-royalty
                                      "trTutorDuration"
                                      "byReportID"
                                      {:key 1})))
          title (:title raw-data)
          data (:reportContent raw-data)
          real-data (reverse
                       (sort-by :duration
                                (pmap #(assoc (dissoc % :duration)
                                              :duration (int (/ (:duration %)
                                                                3600)))
                                      data)))
          final-data (pmap #(assoc %
                                  :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :tutorDuration final-data
         :links (links-duration)}))


(defn duration-data [link-id]
    (let [raw-data (:value
                     (first (cl/get-view cdb-znet-royalty
                                         "trTutorDuration"
                                         "byReportID"
                                         {:key (read-string link-id)})))
          title (:title raw-data)
          data (:reportContent raw-data)
          real-data (reverse
                       (sort-by :duration
                                (pmap #(assoc (dissoc % :duration)
                                              :duration (int (/ (:duration %)
                                                                3600)))
                                      data)))
          final-data (pmap #(assoc %
                                  :rank (inc (.indexOf real-data %)))
                           real-data)]
        {:title title
         :tutorDuration final-data
         :links (links-duration)}))


















