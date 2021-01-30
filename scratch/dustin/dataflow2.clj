(ns dustin.dataflow2
  (:require
    [minitest :refer [tests]]
    [missionary.core :as m]))

;parallel reactor application by s-expression
;parallel reactor application in let by topo sorting of bindings
;sequential reactor evaluation by let is the base case
;reactive-for
;Unknowns:
;Nested bind and tracing through nested bind?
;Control flow special forms - if, case
;Function composition / lambda and nested reactors
;What are we doing next?

(declare dataflow <- cp)

(comment

  (def !a (atom 0))

  (def ast '(+ (inc (<- >a)) (dec (<- >b))))

  (def r (trace/reactor! {'>a (m/watch !a)} (compile ast)))

  (def !trace (trace/log! r))
  @!trace := [{

               }]

  )

(tests

  bind :: a -> Dataflow b
  fmap :: a -> b

  (def !a (atom 10))
  (def !b (atom 100))

  (def ast-foo '(+ (<- (f> (<- >a)))))
  (def ast-bar '(+ (inc (<- >a)) (dec (<- >b))))

  (defn f> [a] (dataflow ast-foo))
  (defn query-popover> [>a] (dataflow ast-foo))
  (defn nothing> [>a >b] (dataflow ast-bar))

  (defn query-popover> [>a] ast-foo)
  (defn nothing> [>a >b] ast-bar)

  (def ast2 '(let [>open (m/watch !a)
                   >b (m/watch !b)]
               (if (true? (<- >open))
                 (query-popover> >a)
                 ~(nothing> >a >b))))
  (eval 'ast2) := 110

  (def sourcemap (vec (tree-seq coll? identity ast2)))

  (sourcemap 0) := '(let [>a (m/watch !a) >b (m/watch !b)] (foo >a >b))

  (binding [*reactor* (dataflow ast2)]

    (get r1 0) := (cp 110)
    )

  ;(def r1 (dataflow ast2))
  ;(def r2 (dataflow ast2))





  )




(comment


  (def >$ (atom ...))
  (def >first (atom ""))
  (def >open (atom false))

  (deftype R [])


  (defn submissions []
    (via (->ContinuousFlow *trace*)
      .
      ))

  (defn query-page [>$ >first >open]
    (via (->ContinuousFlow)
      [(submissions (<- >first))
       (if (<- open)
         (submission-detail "tempid"))]))

  ;(defn query-page [>$ >first >open]
  ;  (binding [*r* (dataflow
  ;                  [(submissions (<- >first))
  ;                   (if (<- open)
  ;                     (submission-detail "tempid"))])]
  ;    (get *r* 0)))

  (binding [*trace* (->Trace)]
    (query-page >$ >first >open))
  := (cp [[{:email "alice"}
           {:email "bob"}]
          {:id "tempid" :email ""}])

  (trace *r*)
  := [{[0] ...}
      {[1] ...}
      {[0 0] ...}
      {}
      {}
      ]

  (reset! >$ ...)

  := (cp [[{:email "alice"}
           {:email "bob"}]
          {:id "tempid" :email "a"}])
  
  )