(ns user.todomvc
  (:require [datascript.core :as d]
            [hfdl.lang :as r :refer [defnode node]]
            [hyperfiddle.photon-dom :as dom]
            [hyperfiddle.rcf :refer [tests ! %]]
            [missionary.core :as m]))

(defnode todos [db status]
  (d/q '[:find [?e ...] :in $ ?status
         :where [?e :task/status ?status]]
       db status))

(defnode root [db]
  (dom/fragment
    (dom/h1 "To Better Do, a distributed concurrent todo list application")
    (let [status (dom/input "")
          es (todos db status)]
      (dom/table
        (dom/thead (r/for [x ["id" "status" "name"]] (dom/td x)))
        (dom/tbody (r/for [e es]
                     (let [{:keys [:db/id
                                   :task/status
                                   :task/description]} (d/entity db e)]
                       (dom/tr e
                         (dom/td :db/id id)
                         (dom/td :task/status (dom/checkbox status))
                         (dom/td :task/description (dom/input description)))))))
      (dom/span "Count: " (count es)))))

(tests
  (def !db (atom (d/db *conn*)))
  (def dispose
    (r/run
      (r/binding [dom/parent (js/document.getElementById "#root")]
        (let [tx (root ~(m/watch !db))]
          (swap! db #(:db-after (d/with % tx)))
          #_(d/transact *conn* tx)))))
  % := _
  (dispose))