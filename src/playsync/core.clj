(ns playsync.core
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!!
                     timeout]]))

(def echo-chan (chan))

(go (println (<! echo-chan)))

(>!! echo-chan "ketchup")

(def buffer-chan (chan 2))

(>!! buffer-chan "why")

(>!! buffer-chan "why")

(def hi-chan (chan))

(doseq [n (range 1000)]
  (go (>! hi-chan (str "hi " n))))

(defn hot-dog-machine []
  (let [get-money (chan) give-hot-dog (chan)]
    (go (<! get-money)
        (>! give-hot-dog "hot dog"))
    [get-money give-hot-dog]))


(let [[in out] (hot-dog-machine)]
  (>!! in "pocket lint")
  (<!! out))

(defn hot-dog-machine-v2
  [hot-dog-stock]
  (let [in (chan) out (chan)]
    (go (loop [hot-dog-count hot-dog-stock]
          (if (> hot-dog-count 0)
            (let [input (<! in)]
              (if (= input 3)
                (do (>! out "hot dog")
                    (recur (dec hot-dog-count)))
                (do (>! out "wilted lettuce")
                    (recur hot-dog-count))))
            (do (close! in)
                (close! out)))))
    [in out]))

(let [[in out] (hot-dog-machine-v2 2)]
  (>!! in "pocket lint")
  (println (<!! out))
  (>!! in 3)
  (println (<!! out))
  (>!! in 3)
  (println (<!! out))
  (>!! in 3)
  (<!! out))

;The in channel of one process is the out of another
(let [ch1 (chan) ch2 (chan) ch3 (chan)]
  (go (>! ch2 (clojure.string/upper-case (<! ch1))))
  (go (>! ch3 (clojure.string/reverse (<! ch2))))
  (go (println (<! ch3)))
  (>!! ch1 "redrum"))

(let [c1 (chan) c2 (chan)]
  (go (<! c2))
  (let [[value channel] (alts!! [c1 [c2 "put!"]])]
    (println value)
    (= channel c2)))


(defn append-to-file
  "Write a string to the end of a file"
  [filename s]
  (spit filename s :append true))

(defn format-quote
  "Delineate the beginning and end of a quote because it's convenient"
  [quote]
  (str "=== BEGIN QUOTE ===\n" quote "=== END QUOTE ===\n\n"))

(defn random-quote
  "Retrieve a random quote and format it"
  []
  (format-quote (slurp "https://www.braveclojure.com/random-quote")))

(defn snag-quotes
  [filename num-quotes]
  (let [ch (chan)]
    (go (while true (append-to-file filename (<! ch))))
    (dotimes [n num-quotes]
      (go (>! ch (random-quote))))))

;The in channel of one process is the out of another one
(defn upper-caser
  [in]
  (let [out (chan)]
    (go (while true (>! out (clojure.string/upper-case (<! in)))))
    out))

(defn reverser
  [in]
  (let [out (chan)]
    (go (while true (>! out (clojure.string/reverse (<! in)))))
    out))

(defn printer
  [in] (go (while true (println (<! in)))))

(def upper-caser-in (chan))
(def upper-caser-out (upper-caser upper-caser-in))
(def reverser-out (reverser upper-caser-out))
(printer reverser-out)

(>!! upper-caser-in "redrum")
(>!! upper-caser-in "repaid")