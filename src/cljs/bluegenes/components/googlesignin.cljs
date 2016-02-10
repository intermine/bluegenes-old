(ns bluegenes.components.googlesignin
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ajax.core :refer [GET POST]]))

(defn handle-authentication
  "Tell re-frame that the user has logged in."
  [res]
  (re-frame/dispatch [:set-user
                      (get res "token")
                      (get res "email")
                      (get res "name")
                      (get res "picture")]))

(defn authenticate-with-server
  "Send a Google ID token to the server for validation."
  [id-token]
  (GET (str "/api/auth/google/authenticate/" id-token)
       {:handler-error
        (fn [er] (.error js/console "Error authenticating Google user." er ))
        :handler
        handle-authentication}))

(defn attach-sign-in
  "Add a click function to the sign-in button that accepts a Google user
  object and sends its token to the server for handling."
  [el auth2]
  (.attachClickHandler auth2
                       el
                       #js{}
                       (fn [user]
                         (authenticate-with-server (aget (.call (aget user "getAuthResponse") user) "id_token")))))

(defn make-google-login
  "Turns a DOM element into a clickable Google login button.
  First pull down the public google client-id from the server."
  [e]
  (GET "/api/auth/google/credentials"
       {:handler (fn [res]
                   (let [cid (get res "client-id")]
                     (.load js/gapi "auth2" (fn ^:export []
                                              (let [auth2 (.init js/gapi.auth2
                                                                 #js{
                                                                     :scope "profile"
                                                                     :fetch_basic_profile true
                                                                     :client_id cid})]
                                                (attach-sign-in e auth2))))))}))

(defn full-name
  "Show the name of the user."
  [v]
  [:span.name (:name @v)])

(defn email
  "Show the email of the user."
  [v]
  [:span.email (:email @v)])

(defn known-user
  "Create a container for showing known user information."
  [v]
  [:div.whoami
   [:span [:i.fa.fa-user " "] [full-name v]]
  ;  [email v]
   ])

(defn unknown-user
  "Create a Google login button."
  []
  (fn []
    (reagent/create-class
     {:reagent-render
      (fn []
        [:div "Sign In"])
      :component-did-mount
      (fn [e]
        (make-google-login (reagent/dom-node e)))})))

(defn main
  "Creates a component that either shows a user's details or a Google login button."
  []
  (let [whoami (re-frame/subscribe [:whoami])]
    (fn []
      (reagent/create-class
       {:reagent-render
        (fn []
           (if (true? (:authenticated @whoami))
             [known-user whoami]
             [unknown-user]))}))))
