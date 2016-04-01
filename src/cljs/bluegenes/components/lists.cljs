(ns bluegenes.components.lists
    (:require [re-frame.core :as re-frame]))


(defn main-view []
  [:main.homepage.lists-page
   [:div.cards
    [:div.step-container
      [:div.body
        [:h3 "Select an existing list"]
         [:div "List selector here"]
        ]]
     [:div.step-container
       [:div.body
        [:h3 "Let's make a new list"]
        [:div "Smart ID resolver here"]
      ]]
    ]
    [:div.step-container
      [:div.body.list-show
       [:div.list
       "All of your list data in a table"
        ]
       [:div.toolbox
        [:div
         [:h5 "Toolbox"]
          [:p [:svg.icon
            [:use {:xlinkHref "#icon-info"}]]
            "If the list looks right to you, analyse it to learn more or save it for later: "]

          [:button "List analysis ->"]]
          [:button
            [:svg.icon.icon-floppy-disk
              [:use {:xlinkHref "#icon-floppy-disk"}]]
                " Save list"]]
      ]]
   ])
