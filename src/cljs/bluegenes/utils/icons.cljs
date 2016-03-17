(ns bluegenes.utils.icons)

;;REGARDING THE USE OF INLINE SVGS, SEE: https://github.com/reagent-project/reagent/issues/121#issuecomment-197805565
;;Right now our workaround is to include a higher version of react in the
;;project.clj. This makes SVG [:use] tags work properly but results in some
;;(seemingly?) harmless console warnings about deprecated methods.
;;if they DO prove problematic, try this method below as a workaround:
'(defn img-with-href [[elem-type props :as elem]]
  (let [image-elem [elem-type (select-keys props #{:href :height :width})]
        image-elem-html (clojure.string/replace
                         (reagent/render-to-static-markup image-elem)
                         "href" "xlink:href")]
    [:svg
     (merge props
            {:dangerouslySetInnerHTML {:__html image-elem-html}})]))

;;TO USE AN SVG INLINE, do it like so:
;;[:svg.icon.icon-share [:use {:xlinkHref "#icon-share"}]]
;;The definitions of icons are below ) use the part after # in the symbol
;;tag as identifiers.

(defn icons []
  [:svg
  {:version "1.1",
   :height "0",
   :width "0",
   :style {:position "absolute" :width 0 :height 0}}
  [:defs
   [:symbol#icon-bar-chart
    {:viewBox "0 0 32 28"}
    [:title "bar-chart"]
    [:path.path1
     {:d
      "M10 14v8h-4v-8h4zM16 6v16h-4v-16h4zM32 24v2h-32v-24h2v22h30zM22 10v12h-4v-12h4zM28 4v18h-4v-18h4z"}]]
   [:symbol#icon-floppy-disk
    {:viewBox "0 0 16 16"}
    [:title "floppy-disk"]
    [:path.path1
     {:d
      "M14 0h-14v16h16v-14l-2-2zM8 2h2v4h-2v-4zM14 14h-12v-12h1v5h9v-5h1.172l0.828 0.828v11.172z"}]]
   [:symbol#icon-user
    {:viewBox "0 0 16 16"}
    [:title "user"]
    [:path.path1
     {:d
      "M9 11.041v-0.825c1.102-0.621 2-2.168 2-3.716 0-2.485 0-4.5-3-4.5s-3 2.015-3 4.5c0 1.548 0.898 3.095 2 3.716v0.825c-3.392 0.277-6 1.944-6 3.959h14c0-2.015-2.608-3.682-6-3.959z"}]]
   [:symbol#icon-cog
    {:viewBox "0 0 16 16"}
    [:title "cog"]
    [:path.path1
     {:d
      "M14.59 9.535c-0.839-1.454-0.335-3.317 1.127-4.164l-1.572-2.723c-0.449 0.263-0.972 0.414-1.529 0.414-1.68 0-3.042-1.371-3.042-3.062h-3.145c0.004 0.522-0.126 1.051-0.406 1.535-0.839 1.454-2.706 1.948-4.17 1.106l-1.572 2.723c0.453 0.257 0.845 0.634 1.123 1.117 0.838 1.452 0.336 3.311-1.12 4.16l1.572 2.723c0.448-0.261 0.967-0.41 1.522-0.41 1.675 0 3.033 1.362 3.042 3.046h3.145c-0.001-0.517 0.129-1.040 0.406-1.519 0.838-1.452 2.7-1.947 4.163-1.11l1.572-2.723c-0.45-0.257-0.839-0.633-1.116-1.113zM8 11.24c-1.789 0-3.24-1.45-3.24-3.24s1.45-3.24 3.24-3.24c1.789 0 3.24 1.45 3.24 3.24s-1.45 3.24-3.24 3.24z"}]]
   [:symbol#icon-download3
    {:viewBox "0 0 16 16"}
    [:title "download3"]
    [:path.path1
     {:d
      "M11.5 7l-4 4-4-4h2.5v-6h3v6zM7.5 11h-7.5v4h15v-4h-7.5zM14 13h-2v-1h2v1z"}]]
   [:symbol#icon-star-empty
    {:viewBox "0 0 16 16"}
    [:title "star-empty"]
    [:path.path1
     {:d
      "M16 6.204l-5.528-0.803-2.472-5.009-2.472 5.009-5.528 0.803 4 3.899-0.944 5.505 4.944-2.599 4.944 2.599-0.944-5.505 4-3.899zM8 11.773l-3.492 1.836 0.667-3.888-2.825-2.753 3.904-0.567 1.746-3.537 1.746 3.537 3.904 0.567-2.825 2.753 0.667 3.888-3.492-1.836z"}]]
   [:symbol#icon-star-full
    {:viewBox "0 0 16 16"}
    [:title "star-full"]
    [:path.path1
     {:d
      "M16 6.204l-5.528-0.803-2.472-5.009-2.472 5.009-5.528 0.803 4 3.899-0.944 5.505 4.944-2.599 4.944 2.599-0.944-5.505 4-3.899z"}]]
   [:symbol#icon-question
    {:viewBox "0 0 16 16"}
    [:title "question"]
    [:path.path1
     {:d
      "M7 11h2v2h-2zM11 4c0.552 0 1 0.448 1 1v3l-3 2h-2v-1l3-2v-1h-5v-2h6zM8 1.5c-1.736 0-3.369 0.676-4.596 1.904s-1.904 2.86-1.904 4.596c0 1.736 0.676 3.369 1.904 4.596s2.86 1.904 4.596 1.904c1.736 0 3.369-0.676 4.596-1.904s1.904-2.86 1.904-4.596c0-1.736-0.676-3.369-1.904-4.596s-2.86-1.904-4.596-1.904zM8 0v0c4.418 0 8 3.582 8 8s-3.582 8-8 8c-4.418 0-8-3.582-8-8s3.582-8 8-8z"}]]
   [:symbol#icon-plus
    {:viewBox "0 0 16 16"}
    [:title "plus"]
    [:path.path1
     {:d
      "M15.5 6h-5.5v-5.5c0-0.276-0.224-0.5-0.5-0.5h-3c-0.276 0-0.5 0.224-0.5 0.5v5.5h-5.5c-0.276 0-0.5 0.224-0.5 0.5v3c0 0.276 0.224 0.5 0.5 0.5h5.5v5.5c0 0.276 0.224 0.5 0.5 0.5h3c0.276 0 0.5-0.224 0.5-0.5v-5.5h5.5c0.276 0 0.5-0.224 0.5-0.5v-3c0-0.276-0.224-0.5-0.5-0.5z"}]]
   [:symbol#icon-minus
    {:viewBox "0 0 16 16"}
    [:title "minus"]
    [:path.path1
     {:d
      "M0 6.5v3c0 0.276 0.224 0.5 0.5 0.5h15c0.276 0 0.5-0.224 0.5-0.5v-3c0-0.276-0.224-0.5-0.5-0.5h-15c-0.276 0-0.5 0.224-0.5 0.5z"}]]
   [:symbol#icon-external
    {:viewBox "0 0 16 16"}
    [:title "external link"]
    [:path.path1
     {:d
      "M4 10c0 0 0.919-3 6-3v3l6-4-6-4v3c-4 0-6 2.495-6 5zM11 12h-9v-6h1.967c0.158-0.186 0.327-0.365 0.508-0.534 0.687-0.644 1.509-1.135 2.439-1.466h-6.914v10h13v-4.197l-2 1.333v0.864z"}]]]])
