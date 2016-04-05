(ns bluegenes.components.paginator
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn abs
  "Get the absolute value of a number while avoiding
  javascript's abs() function."
  [n] (max n (- n)))

(defn shift-right
  "If the beginning of a vector falls below the minimum (mi) then shift the
  vector to the right by the difference."
  [v mi]
  (if (< (first v) mi)
    (map #(+ % (inc (abs (first v)))) v)
    v))

(defn shift-left
  "If the end of a vector falls above the maximum (ma) then shift the
  vector to the left by the difference."
  [v ma]
  (if (> (last v) ma)
    (map #(- % (- (last v) ma)) v)
    v))

(defn keep-in-range
  "Shift a vector left and then right so that it falls between a range.
  This will break if the mi and ma values are between the first and last
  value of the vector."
  [v mi ma]
  (-> (shift-right v mi)
      (shift-left ma)))

(defn get-numbers-around
  "Get a range around a number +/- the spread adjusted to fall within a range."
  [val spread [mi ma]]
  (println "get-numbers around called with " ma)
  (println "val is" val)
  (-> (range (- val spread) (+ val (inc spread)))
      (keep-in-range mi ma)))

(defn dropdown
  "Not currently used but keeping as an idea. This is meant
  to provide a page selection drop down. It's too long when dealing
  with many results. Perhaps provide a partitioned list?"
  [total]
  [:div.dropdown.fill-width
   [:button.btn.btn-primary.dropdown-toggle
    {:type "button"
     :data-toggle "dropdown"} "Example"]
   [:ul.dropdown-menu
    (for [n (range 1 total 20)]
      [:li (str "Page " n)])]])

(defn update-both
  "Update the local state and then call back to the instantiator
  with the updated page value."
  [local-state handlerfn n]
  (handlerfn (:current-page (swap! local-state assoc :current-page n))))

(defn main
  "Create pagination for a list of things.
  :rows The total number of rows in the document
  :rows-per-page The number of rows in a page.
  :spread The number of pages to show +/- the current page.
  :on-change A function to call with the current page number.
  TODO: Move the selection handlers to their own functions and use let[]
  to reduce calculations on large arrays."
  [{:keys [spread rows rows-per-page on-change current-page] :as s}]
  (let [state (reagent/atom {:current-page current-page
                             :spread spread
                             :rows-per-page rows-per-page})
        updater (partial update-both state on-change)]
    (reagent/create-class
     {:display-name "bluegenes.components.paginator/main"

      :reagent-render (fn [{:keys [spread rows rows-per-page on-change current-page]}]
        (println "args for paginator" rows)
                        [:div.noselect
                         [:nav
                          [:ul.pagination
                           ^{:key "first"} [:li
                                            {:class (if
                                                      (= (:current-page @state) 1)
                                                      "disabled")}
                                            [:a {:on-click
                                                 (if
                                                   (> (:current-page @state) 1)
                                                   #(updater 1))}
                                             [:i.fa.fa-step-backward]]]
                           ^{:key "prev"} [:li
                                           {:class (if
                                                     (= (:current-page @state) 1)
                                                     "disabled")}
                                           [:a {:on-click
                                                (if (> (:current-page @state) 1)
                                                  #(updater (dec (:current-page @state))))}
                                            [:i.fa.fa-arrow-left]]]
                           (doall
                             (for [n (get-numbers-around
                                      (:current-page @state)
                                      (:spread @state)
                                      [1 (inc (count (partition (:rows-per-page @state) (range 1 rows))))])]
                               ^{:key n} [:li {:class (if (= n (:current-page @state))
                                                        "active")}
                                          [:a {:on-click #(updater n)}
                                           (str n)]]))

                           ^{:key "next"} [:li
                                           [:span {:on-click
                                                   #(updater (inc (:current-page @state)))}
                                            [:i.fa.fa-arrow-right]]]
                           ^{:key "last"} [:li
                                           [:a {:on-click
                                                #(updater (inc (count (partition (:rows-per-page @state) (range 1 rows)))))}
                                            [:i.fa.fa-step-forward]]]]]
                         [:h4 (str "Showing rows "
                                   (inc (* (dec (:current-page @state)) (:rows-per-page @state)))
                                   " to "
                                   (* (:current-page @state) (:rows-per-page @state)))]])})))
