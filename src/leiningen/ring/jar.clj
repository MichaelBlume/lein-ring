(ns leiningen.ring.jar
  (:require
    [clojure.string :as str]
    [leiningen.jar :as lein-jar]
    [leiningen.ring
     [util :refer [compile-form ensure-handler-set!
                   update-project generate-resolve]]
     [server :refer [add-server-dep]]]))

(defn default-main-namespace [project]
  (let [handler-sym (get-in project [:ring :handler])]
    (str (namespace handler-sym) ".main")))

(defn main-namespace [project]
  (or (get-in project [:ring :main])
      (default-main-namespace project)))

(defn compile-main [project]
  (let [main-ns (symbol (main-namespace project))
        options (-> (select-keys project [:ring])
                    (assoc-in [:ring :open-browser?] false)
                    (assoc-in [:ring :stacktraces?] false)
                    (assoc-in [:ring :auto-reload?] false))]
    (compile-form project main-ns
      `(do (ns ~main-ns
             (:gen-class))
           (defn ~'-main []
             (~(generate-resolve 'ring.server.leiningen/serve) '~options))))))

(defn add-main-to-aot [aot-list main-ns]
  (if (= aot-list [:all])
    aot-list
    (-> aot-list (conj main-ns) distinct)))

(defn add-main-class [project]
  (let [main (symbol (main-namespace project))]
    (->
      project
      (update-project assoc :main main)
      (update-project update-in [:aot] add-main-to-aot main))))

(defn jar
  "Create an executable $PROJECT-$VERSION.jar file."
  [project]
  (ensure-handler-set! project)
  (let [project (-> project add-server-dep add-main-class)]
    (compile-main project)
    (lein-jar/jar project)))
