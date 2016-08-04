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
;;[:svg.icon.icon-plus [:use {:xlinkHref "#icon-plus"}]]
;;The definitions of icons are below ) use the part after # in the symbol
;;tag as identifiers.

(defn icons []
  [:svg
  {:version "1.1",
   :height "0",
   :width "0",
   :style {:position "absolute" :width 0 :height 0}}
  [:defs
   [:symbol#uparrow
    {:viewBox "0 0 320 320"}
    [:title "uparrow"]
    [:path.path1
     {:d
      "M234.991,319.982c2.618,0,5.22-1.078,7.071-2.929s2.929-4.453,2.929-7.071v-140h65 c3.922-0.008,7.721-2.552,9.221-6.176s0.61-8.109-2.159-10.886l-150-150C165.202,1.074,162.604,0,159.991,0 c-2.614,0-5.212,1.074-7.062,2.92l-150,150c-2.769,2.777-3.659,7.263-2.159,10.886c1.5,3.624,5.299,6.168,9.221,6.176h65v140 c0,2.618,1.078,5.22,2.929,7.071s4.453,2.929,7.071,2.929H234.991z"}]
    ]
   [:symbol#leftturn
    {:viewBox "0 0 32 28"}
    [:title "leftturn"]
    [:path.path1
     {:d
      "M10.385,1.499C10.096,1.378,9.76,1.446,9.537,1.667L0.228,8.98c-0.304,0.302-0.304,0.797,0,1.1\n\t\t\tl9.309,7.278c0.148,0.15,0.348,0.229,0.549,0.229c0.102,0,0.203-0.021,0.299-0.059c0.291-0.121,0.465-0.404,0.465-0.719V12.45\n\t\t\tc9.955,0,14.309,2.808,13.646,11.751C28.713,9.893,20.619,6.659,10.85,6.659V2.217C10.849,1.902,10.676,1.619,10.385,1.499z"}]
    ]
   [:symbol#microscope
    {:viewBox "0 0 32 28"}
    [:title "microscope"]
    [:path.path1
     {:d
      "M10.171,11.564c-0.144,0.298-0.163,0.642-0.054,0.955s0.34,0.569,0.639,0.713l1.11,0.532\n\t\t\tc0.619,0.297,1.363,0.038,1.663-0.581l0.111-0.229l0.14,0.066c0.619,0.297,1.362,0.036,1.662-0.582l1.668-3.447\n\t\t\tc0.914,0.512,1.871,1.208,2.551,2.123c0.94-0.857,2.188-1.377,3.56-1.377c0.138,0,0.271,0.007,0.403,0.017\n\t\t\tc-1.143-2.087-3.051-3.504-4.745-4.417l0.739-1.53c0.145-0.299,0.164-0.644,0.056-0.956c-0.108-0.313-0.339-0.57-0.64-0.714\n\t\t\tl-0.131-0.063c0.094-0.195,0.106-0.419,0.035-0.623c-0.07-0.204-0.221-0.371-0.414-0.465L16.633,0.08\n\t\t\tc-0.406-0.194-0.894-0.024-1.089,0.382l-0.142-0.068c-0.62-0.297-1.363-0.036-1.663,0.582L9.564,9.61\n\t\t\tC9.42,9.909,9.4,10.253,9.509,10.565c0.11,0.313,0.34,0.57,0.639,0.714l0.131,0.062L10.171,11.564z"}]
    [:path.path2
     {:d
      "M19.669,18.9c-0.215,0.391-0.467,0.77-0.778,1.119c-0.758,0.848-1.771,1.443-3.05,1.785v-1.226h1.038v-1.104\n\t\t\tc0-0.449-0.364-0.812-0.813-0.812H7.304c-0.449,0-0.812,0.363-0.812,0.812v1.104h1.799h0.038v6.363\n\t\t\tc-0.013,0.006-0.025,0.011-0.038,0.017c-1.999,0.803-3.648,2.25-4.659,4.166h16.134c-0.887-1.677-2.262-2.988-3.93-3.834\n\t\t\tc-0.038-0.021-0.077-0.037-0.115-0.058c0.041,0.021,0.08,0.036,0.12,0.058v-1.33c2.479-0.453,4.552-1.537,6.074-3.244\n\t\t\tc0.688-0.772,1.209-1.609,1.603-2.455c-0.098,0.006-0.196,0.008-0.296,0.008C21.854,20.27,20.606,19.75,19.669,18.9z"}]
    [:path.path3
     {:d
      "M23.22,10.75c-2.354,0-4.271,1.908-4.271,4.254c0,2.345,1.918,4.251,4.271,4.251c2.357,0,4.273-1.906,4.273-4.251\n\t\t\tC27.493,12.658,25.579,10.75,23.22,10.75z"}]]
   [:symbol#molecule
    {:viewBox "0 0 32 28"}
    [:title "molecule"]
    [:path.path1
     {:d
      "M31.814,13.411c0-1.133-0.916-2.049-2.048-2.049c-1.131,0-2.048,0.916-2.048,2.049c0,0.538,0.21,1.026,0.55,1.392\n\t\tl-0.852,1.444c-0.142-0.031-0.287-0.049-0.438-0.049c-0.828,0-1.539,0.492-1.861,1.201h-2.029\n\t\tc-0.301-0.656-0.934-1.127-1.686-1.191l-0.883-1.509c0.285-0.354,0.457-0.801,0.457-1.288c0-0.468-0.158-0.896-0.421-1.24\n\t\tl0.902-1.536c0.024,0.001,0.048,0.003,0.073,0.003c1.131,0,2.047-0.916,2.047-2.047s-0.916-2.048-2.047-2.048\n\t\ts-2.049,0.917-2.049,2.048c0,0.44,0.141,0.848,0.379,1.183l-0.92,1.589c-0.004,0-0.009,0-0.014,0c-0.836,0-1.553,0.501-1.871,1.217\n\t\tl-2.278,0.013c-0.315-0.723-1.034-1.227-1.872-1.229l-0.898-1.551c0.354-0.369,0.573-0.868,0.573-1.42\n\t\tc0-0.474-0.161-0.906-0.431-1.254l0.847-1.46c1.086-0.05,1.952-0.944,1.952-2.044c0-1.131-0.917-2.047-2.048-2.047\n\t\ts-2.048,0.916-2.048,2.047c0,0.511,0.188,0.977,0.497,1.335l-0.812,1.375c-0.001,0-0.003,0-0.005,0c-0.87,0-1.611,0.544-1.908,1.31\n\t\tH6.269c-0.341-0.659-1.027-1.11-1.819-1.11c-1.131,0-2.049,0.917-2.049,2.048S3.319,10.64,4.45,10.64\n\t\tc0.786,0,1.468-0.442,1.812-1.094h2.58c0.317,0.466,0.823,0.792,1.407,0.874l1.03,1.746c-0.266,0.346-0.425,0.776-0.425,1.247\n\t\tc0,0.492,0.175,0.944,0.465,1.297l-0.882,1.495c-0.786,0.036-1.456,0.516-1.766,1.196H6.312C5.989,16.692,5.278,16.2,4.45,16.2\n\t\tc-1.131,0-2.049,0.917-2.049,2.048c0,0.494,0.177,0.949,0.47,1.305l-0.894,1.561C0.879,21.151,0,22.051,0,23.159\n\t\tc0,1.131,0.917,2.047,2.048,2.047s2.048-0.916,2.048-2.047c0-0.494-0.174-0.946-0.465-1.299l0.891-1.565\n\t\tc0.719-0.026,1.342-0.421,1.688-1.003h2.564c0.32,0.539,0.879,0.92,1.532,0.992l0.956,1.651c-0.255,0.341-0.408,0.763-0.408,1.224\n\t\tc0,0.511,0.188,0.978,0.498,1.336l-0.95,1.642c-1.069,0.067-1.915,0.954-1.915,2.04c0,1.132,0.916,2.049,2.047,2.049\n\t\ts2.048-0.917,2.048-2.049c0-0.529-0.202-1.01-0.532-1.373l0.502-0.867c0,0,0.001,0.002,0.002,0.002l0.426-0.734\n\t\tc0.762-0.028,1.416-0.473,1.744-1.109h2.384c0.329,0.643,0.987,1.084,1.752,1.109l0.896,1.55c-0.357,0.368-0.579,0.87-0.579,1.424\n\t\tc0,1.132,0.917,2.049,2.049,2.049s2.048-0.917,2.048-2.049c0-1.06-0.808-1.933-1.84-2.036l-0.951-1.65\n\t\tc0.309-0.357,0.494-0.821,0.494-1.33c0-0.463-0.154-0.885-0.412-1.229l0.965-1.657c0.619-0.092,1.148-0.461,1.457-0.98h2.233\n\t\tc0.356,0.601,1.011,1.006,1.761,1.006c1.131,0,2.047-0.917,2.047-2.049c0-0.301-0.065-0.586-0.183-0.842l1.123-1.957\n\t\tC31.004,15.347,31.814,14.473,31.814,13.411z M18.928,21.179c-0.843,0-1.566,0.437-1.881,1.164h-2.266\n\t\tc-0.314-0.728-1.038-1.235-1.88-1.235c-0.024,0-0.048,0.002-0.073,0.004l-0.82-1.445c0.354-0.367,0.573-0.869,0.573-1.42\n\t\tc0-0.521-0.196-0.996-0.517-1.356l0.829-1.433c0.003,0,0.005,0.001,0.008,0.001c0.794,0,1.481-0.453,1.821-1.114h2.384\n\t\tc0.328,0.64,0.982,1.082,1.743,1.109l0.829,1.452c-0.312,0.359-0.502,0.828-0.502,1.342c0,0.559,0.225,1.101,0.586,1.471\n\t\tl-0.831,1.463C18.931,21.179,18.929,21.179,18.928,21.179z"}]]
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
   [:symbol#icon-download
    {:viewBox "0 0 16 16"}
    [:title "download"]
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
      "M4 10c0 0 0.919-3 6-3v3l6-4-6-4v3c-4 0-6 2.495-6 5zM11 12h-9v-6h1.967c0.158-0.186 0.327-0.365 0.508-0.534 0.687-0.644 1.509-1.135 2.439-1.466h-6.914v10h13v-4.197l-2 1.333v0.864z"}]]
  [:symbol#icon-waiting
    {:viewBox "0 0 16 16"}
    [:title "waiting"]
    [:path.path1
     {:d
      "M6 2c0-1.105 0.895-2 2-2s2 0.895 2 2c0 1.105-0.895 2-2 2s-2-0.895-2-2zM10.243 3.757c0-1.105 0.895-2 2-2s2 0.895 2 2c0 1.105-0.895 2-2 2s-2-0.895-2-2zM13 8c0-0.552 0.448-1 1-1s1 0.448 1 1c0 0.552-0.448 1-1 1s-1-0.448-1-1zM11.243 12.243c0-0.552 0.448-1 1-1s1 0.448 1 1c0 0.552-0.448 1-1 1s-1-0.448-1-1zM7 14c0 0 0 0 0 0 0-0.552 0.448-1 1-1s1 0.448 1 1c0 0 0 0 0 0 0 0.552-0.448 1-1 1s-1-0.448-1-1zM2.757 12.243c0 0 0 0 0 0 0-0.552 0.448-1 1-1s1 0.448 1 1c0 0 0 0 0 0 0 0.552-0.448 1-1 1s-1-0.448-1-1zM2.257 3.757c0 0 0 0 0 0 0-0.828 0.672-1.5 1.5-1.5s1.5 0.672 1.5 1.5c0 0 0 0 0 0 0 0.828-0.672 1.5-1.5 1.5s-1.5-0.672-1.5-1.5zM0.875 8c0-0.621 0.504-1.125 1.125-1.125s1.125 0.504 1.125 1.125c0 0.621-0.504 1.125-1.125 1.125s-1.125-0.504-1.125-1.125z"}]]

    [:symbol#icon-search
     {:viewBox "0 0 16 16"}
     [:title "search"]
     [:path.path1
      {:d
       "M15.504 13.616l-3.79-3.223c-0.392-0.353-0.811-0.514-1.149-0.499 0.895-1.048 1.435-2.407 1.435-3.893 0-3.314-2.686-6-6-6s-6 2.686-6 6 2.686 6 6 6c1.486 0 2.845-0.54 3.893-1.435-0.016 0.338 0.146 0.757 0.499 1.149l3.223 3.79c0.552 0.613 1.453 0.665 2.003 0.115s0.498-1.452-0.115-2.003zM6 10c-2.209 0-4-1.791-4-4s1.791-4 4-4 4 1.791 4 4-1.791 4-4 4z"}]]

     [:symbol#icon-info
      {:viewBox "0 0 18 18"}
      [:title "info"]
      [:path.path1
       {:d
        "M7 4.75c0-0.412 0.338-0.75 0.75-0.75h0.5c0.412 0 0.75 0.338 0.75 0.75v0.5c0 0.412-0.338 0.75-0.75 0.75h-0.5c-0.412 0-0.75-0.338-0.75-0.75v-0.5z"}]
      [:path.path2 {:d "M10 12h-4v-1h1v-3h-1v-1h3v4h1z"}]
      [:path.path3
       {:d
        "M8 0c-4.418 0-8 3.582-8 8s3.582 8 8 8 8-3.582 8-8-3.582-8-8-8zM8 14.5c-3.59 0-6.5-2.91-6.5-6.5s2.91-6.5 6.5-6.5 6.5 2.91 6.5 6.5-2.91 6.5-6.5 6.5z"}]]

        [:symbol#icon-arrow-right
       {:viewbox "0 0 16 16"}
       [:title "arrow-right"]
       [:path.path1 {:d "M15.5 8l-7.5-7.5v4.5h-8v6h8v4.5z"}]]

   ]])
