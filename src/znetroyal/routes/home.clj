(ns znetroyal.routes.home
  (:use compojure.core)
  (:require [znetroyal.views.layout :as view]
            [znetroyal.util :as util]
            [ring.util.response :refer [redirect]]
            [noir.session :as session]
            [znetroyal.dbprocess.maindb :as main-db]
            [znetroyal.dbprocess.reportdb :as report-db]
            [liberator.core :refer [defresource resource request-method-in]]
            [znetroyal.dbprocess.restdb :as lib]))



; Some global symbols to process the login, for development purpose only
(def global-password "zenimomos123")
(def global-user "admin123")


; These are pages in the form of clojure functions

(defn login-page [msg]
  "This is the entry point of the application, where people should login"
      (view/render "login.html"
                   {:message msg}))

(defn home-page []
  "Once you logged in, then you can start seeing reports here, basically the homepage of backoffice"
      (view/render "categories.html"
                   {:message "Tutor yang rajin.. disayang NCEN"}))

(defn royalty-page [link-id]
    (if (nil? link-id)
        (view/render "royalty.html"
                     {:message "Total video played by each tutor"
                      :webdata (report-db/init-royalty-data)})
        (view/render "royalty.html"
                     {:message "Total video played by each tutor"
                      :webdata (report-db/royalty-data link-id)})))

(defn topchart-page [link-id1 link-id2]
    (if (nil? link-id1)
        (view/render "topchart.html"
                     {:message "Best performing content during a certain period"
                      :webdata (report-db/init-topchart-data)})
        (view/render "topchart.html"
                     {:message "Best performing content during a certain period"
                      :webdata (report-db/topchart-data link-id1 link-id2)})))

(defn categories-page []
    (view/render "categories.html"
                 {:message "Best performing Level 1 & Level 2 categories"}))

(defn topchart-categories-page [cg-level cg-id time-frame]
    (view/render "topchartcategories.html"
                 {:message "Best performing content in each categories"
                  :webdata (report-db/topchart-categories (read-string cg-level)
                                                          (read-string cg-id)
                                                          (read-string time-frame))}))

(defn duration-page [link-id]
    (if (nil? link-id)
        (view/render "duration.html"
                     {:message "Total video duration by each tutor"
                      :webdata (report-db/init-duration-data)})
        (view/render "duration.html"
                     {:message "Total video duration by each tutor"
                      :webdata (report-db/duration-data link-id)})))

; Liberator part to generate json objects required for angular to work


(defresource lib-catdata [requestedData]
  :service-available? true
  :allowed-methods [:get]
  :handle-ok (lib/get-data requestedData)
  :etag "fixed-etag"
  :available-media-types ["application/json"])

(defresource lib-report-data [level timeframe]
  :service-available? true
  :allowed-methods [:get]
  :handle-ok (lib/get-report (read-string level) (read-string timeframe))
  :etag "fixed-etag"
  :available-media-types ["application/json"])



; OK all routes go here

(defroutes home-routes
  ; This is the entry route where visitors must login first
  (GET "/" []
       (login-page "Please enter your credential Sir!"))

  ; If the login failed then this is where he/she will be redirected
  (GET "/try-login" []
       (login-page "Salah masukin credential dol!"))

  ; To actually process the login information from the login homepage
  (POST "/act-login" [username pwd]
        (if (and (= username global-user)
                 (= pwd global-password))
            (do (session/put! :user username)
                (redirect "/home"))
            (redirect "/try-login")))

  ; The home page after login where tutor can start working
  (GET "/home" request
       (if (= global-user (session/get :user))
           (do (println (:session request)) (home-page))
           (login-page "We don't yet know who you are, so put your creds first")))

  ; Reporting page for tutor royalty (at the moment only video played)
  (GET "/royalty" [linkID]
       (if (= global-user (session/get :user))
           (royalty-page linkID)
           (login-page "We don't yet know who you are, so put your creds first")))

  (GET "/topchart" [linkID1 linkID2]
       (if (= global-user (session/get :user))
           (topchart-page linkID1 linkID2)
           (login-page "We don't yet know who you are, so put your creds first")))

  (GET "/categories" request
       (if (= global-user (session/get :user))
           (categories-page)
           (login-page "I've told you to sign in, haven't i?")))

  (GET "/catdata" [requestedData]
       (if (= global-user (session/get :user))
           (lib-catdata requestedData)
           (login-page "I've told you to sign in, haven't I?")))

  (GET "/reportdata" [level timeframe]
       (lib-report-data level timeframe))

  (GET "/topchartcategories" [cgLevel cgID timeframe]
       (if (= global-user (session/get :user))
           (topchart-categories-page cgLevel cgID timeframe)
           (login-page "We don't yet know who you are, so put your creds first")))

  (GET "/duration" [linkID]
       (if (= global-user (session/get :user))
           (duration-page linkID)
           (login-page "We don't yet know who you are, so put your creds first")))

  (GET "/checksession" request
       (str request))

  ; As the name implies
  (GET "/logout" [] (do (session/clear!)
                        (login-page "And there's no other way than to saaayyy goodbye.."))))























