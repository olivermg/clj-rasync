(defproject clj-rasync "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.442"]
                 [com.stuartsierra/component "0.3.2"]
                 [integrant "0.4.0"]
                 [http-kit "2.2.0"]
                 [http.async.client "1.2.0"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj"]
  ;;;:java-source-paths ["src/java"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]

  :target-path "target/%s"

  :profiles {:repl {:repl-options {:init-ns user}}

             :dev {:source-paths ["dev/src"]
                   :resource-paths ["dev/resources"]
                   :dependencies [[integrant/repl "0.2.0"]]}}

  )
