(ns org.broadinstitute.uicomps.modal
  (:require
   [dmohs.react :as r]
   [linked.core :as linked]
   ))

(defonce ^:private instance nil)

(def body-class "broadinstitute-modal-open")

(def default-props
  {:z-index nil
   :overlay-color "rgba(110,110,110,0.4)"
   :modal-background-color "white"})

(defn- calculate-scroll-bar-width []
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
        (- width-without-scrollbar width-with-scrollbar)))))

(defn- construct-css []
  (str "
body." body-class " {
  overflow: hidden;
  padding-right: " (calculate-scroll-bar-width) "px;
}
"))

(def css-element-id (subs (str ::modal-css) 1))

(defn- add-css []
  (let [e (js-invoke js/document "createElement" "style")]
    (aset e "innerHTML" (construct-css))
    (aset e "id" css-element-id)
    (js-invoke (aget js/document "head") "appendChild" e)))

(defn- remove-css []
  (js-invoke (js-invoke js/document "getElementById" css-element-id) "remove"))

(r/defc Container
  {:push-modal
   (fn [{:keys [props state]} id content]
     (when (empty? (:stack @state))
       (add-css))
     (swap! state update :stack assoc id content)
     (js-invoke (aget js/document "body" "classList") "add" body-class))
   :remove-modal
   (fn [{:keys [state after-update]} id]
     (swap! state update :stack dissoc id)
     (after-update
      (fn []
        (when (empty? (:stack @state))
          (js-invoke (aget js/document "body" "classList") "remove" body-class)
          (remove-css)))))
   :get-default-props (constantly default-props)
   :get-initial-state (constantly {:stack {}})
   :render
   (fn [{:keys [props state]}]
     [:div {:className "dummy-class-1"}
      (let [{:keys [stack]} @state]
        (map (fn [[id content]]
               [:div {:style {:position "fixed" :z-index (:z-index props)
                              :top 0 :bottom 0 :left 0 :right 0
                              :background-color (:overlay-color props)
                              :overflow "auto"}}
                [:div {:style {:padding "2rem 0"
                               :display "flex" :justify-content "center" :align-items "flex-start"}}
                 [:div {:style {:background-color (:modal-background-color props)
                                :max-width "95%" :min-width 500}}
                  content]]])
             stack))])
   :component-did-mount
   (fn [{:keys [this]}]
     (set! instance this))
   :component-will-unmount
   (fn [{:keys [this]}]
     (set! instance nil))})

(r/defc Modal
  {:render
   (fn [{:keys [props]}]
     nil)
   :component-will-mount
   (fn [{:keys [props locals]}]
     (swap! locals assoc :id (gensym "modal-"))
     (instance :push-modal (:id @locals) (:content props))
     (when-let [dismiss (:dismiss props)]
       (swap! locals assoc :keydown-handler (fn [e] (when (= 27 (aget e "keyCode")) (dismiss))))
       (.addEventListener js/window "keydown" (:keydown-handler @locals))))
   :component-will-receive-props
   (fn [{:keys [next-props locals]}]
     (instance :push-modal (:id @locals) (:content next-props)))
   :component-will-unmount
   (fn [{:keys [locals]}]
     (when (:keydown-handler @locals)
       (.removeEventListener js/window "keydown" (:keydown-handler @locals)))
     (instance :remove-modal (:id @locals)))})

(defn render
  ([content] (render nil content))
  ([dismiss content]
   (let [element (if (r/valid-element? content) content (r/create-element content))]
     [Modal {:content element :dismiss dismiss}])))
