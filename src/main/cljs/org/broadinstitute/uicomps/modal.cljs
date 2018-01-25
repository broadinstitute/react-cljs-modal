(ns org.broadinstitute.uicomps.modal
  (:require
   [dmohs.react :as r]
   [linked.core :as linked]
   ))

(defonce ^:private stack-size (atom 0))

(def body-class "broadinstitute-modal-open")

(def default-props
  {:z-index nil
   :overlay-color "rgba(110,110,110,0.4)"
   :modal-background-color "white"})

(defn- is-scroll-bar-visible? []
  (> (.. js/document -body -scrollHeight) (.. js/window -innerHeight)))

(defn- calculate-scroll-bar-width []
  (if-not (is-scroll-bar-visible?)
    0
    ;; https://stackoverflow.com/questions/8701754/just-disable-scroll-not-hide-it
    (let [outer (js-invoke js/document "createElement" "div")
          inner (js-invoke js/document "createElement" "div")]
      (aset outer "style" "visibility" "hidden")
      (aset outer "style" "width" "100px")
      (aset inner "style" "width" "100%")
      (aset outer "style" "msOverflowStyle" "scrollbar") ; needed for WinJS apps
      (js-invoke (aget js/document "body") "appendChild" outer)
      (let [width-without-scrollbar (aget outer "offsetWidth")]
        (aset outer "style" "overflow" "scroll")
        (js-invoke outer "appendChild" inner)
        (let [width-with-scrollbar (aget inner "offsetWidth")]
          (js-invoke (aget outer "parentNode") "removeChild" outer)
          (- width-without-scrollbar width-with-scrollbar))))))

(r/defc Modal
  {:get-default-props (constantly default-props)
   :component-will-mount
   (fn [{:keys [locals props]}]
     (let [id (gensym "modal-")
           container (js/document.createElement "div")]
       (js/document.body.appendChild container)
       (swap! locals assoc :container container)
       (swap! stack-size inc)
       (when (pos? @stack-size)
         (js/document.body.classList.add body-class))))
   :render
   (fn [{:keys [props locals]}]
     (let [{:keys [z-index overlay-color modal-background-color content]} props]
       (r/create-portal
        [:div {:style {:position "fixed" :z-index z-index
                       :top 0 :bottom 0 :left 0 :right 0
                       :background-color overlay-color
                       :overflow "auto"}}
         [:div {:style {:padding "2rem 0"
                        :display "flex" :justify-content "center" :align-items "flex-start"}}
          [:div {:style {:background-color modal-background-color
                         :max-width "95%" :min-width 500}}
           content]]]
        (:container @locals))))
   :component-did-mount
   (fn [{:keys [props locals after-update]}]
     (let [{:keys [did-mount dismiss]} props]
       (when did-mount
         (after-update did-mount))
       (when-let [dismiss (:dismiss props)]
         (swap! locals assoc :keydown-handler (fn [e] (when (= 27 (aget e "keyCode")) (dismiss))))
         (.addEventListener js/window "keydown" (:keydown-handler @locals)))))
   :component-will-unmount
   (fn [{:keys [locals]}]
     (.remove (:container @locals))
     (swap! stack-size dec)
     (when (zero? @stack-size)
       (js/document.body.classList.remove body-class))
     (when (:keydown-handler @locals)
       (.removeEventListener js/window "keydown" (:keydown-handler @locals))))})

(defn render [content-or-map]
  (let [m? (map? content-or-map)
        content (if m? (:content content-or-map) content-or-map)
        options (if m? (dissoc content-or-map :content) {})
        element (if (r/valid-element? content) content (r/create-element content))]
    [Modal (assoc options :content element)]))

(let [e (js-invoke js/document "createElement" "style")]
  (aset e "innerHTML"
        (str "body." body-class " {"
             "overflow: hidden;"
             "padding-right: " (calculate-scroll-bar-width) "px;}"))
  (js-invoke (aget js/document "head") "appendChild" e))
