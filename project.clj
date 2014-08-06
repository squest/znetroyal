(defproject znetroyal "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lib-noir "0.8.1"]
                 [compojure "1.1.6"]
                 [ring-server "0.3.1"]
                 [selmer "0.6.1"]
                 [com.taoensso/timbre "3.0.0"]
                 [com.taoensso/tower "2.0.1"]
                 [markdown-clj "0.9.41"]
                 [environ "0.4.0"]
                 [liberator "0.10.0"]
                 [cheshire "5.2.0"]
                 [com.ashafa/clutch "0.4.0-RC1"]
                 [korma "0.3.0-RC6"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [incanter/incanter-core "1.4.1"]
                 [incanter/incanter-excel "1.4.1"]]

  :repl-options {:init-ns znetroyal.repl}
  :plugins [[lein-ring "0.8.10"]
            [lein-environ "0.4.0"]
            [lein-immutant "1.2.1"]]
  :immutant {:context-path "/royalty/"}
  :ring {:handler znetroyal.handler/app
         :init    znetroyal.handler/init
         :destroy znetroyal.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.2.1"]]
         :env {:dev true}}}
  :min-lein-version "2.0.0")
