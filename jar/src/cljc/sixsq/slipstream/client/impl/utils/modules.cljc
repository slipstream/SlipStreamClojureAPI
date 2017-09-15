(ns ^{:no-doc true} sixsq.slipstream.client.impl.utils.modules
  "Utilities specific to working with modules.")

(defn extract-children [module]
  (if-let [children (get-in module [:projectModule :children :item])]
    (let [children (if (map? children) [children] children)] ;; may be single item!
      (map :name children))))

(defn fix-module-name [mname]
  (first (map second (re-seq #"module/(.+)/[\d+]+" mname))))

(defn extract-xml-children [xml]
  (->> (re-seq #"resourceUri=\"([^\"]*)\"" xml)
       (map second)
       (map fix-module-name)))
