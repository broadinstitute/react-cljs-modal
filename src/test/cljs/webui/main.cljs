(ns webui.main
  (:require
   [dmohs.react :as r]
   [org.broadinstitute.uicomps.modal :as modal]
   ))

(defn render-small-modal [dismiss]
  [:div {} "I am a modal." [:br] [:button {:on-click dismiss} "Close"]])

(r/defc SuperLongModal
  {:render
   (fn [{:keys [props]}]
     [:div {}
      "Top of Modal (ESC to close)"
      [:div {:style {:margin-top "200vh"}} "Bottom of Modal"]])})

(r/defc App
  {:render
   (fn [{:keys [this state]}]
     [:div {}
      (when (:small-modal? @state)
        (modal/render (render-small-modal #(swap! state dissoc :small-modal?))))
      (when (:large-modal? @state)
        (modal/render
         {:dismiss #(swap! state dissoc :large-modal?)
          :content [SuperLongModal]}))
      [:div {} "Top of page"]
      [:button {:on-click #(swap! state assoc :small-modal? true)} "Open"]
      [:div {:style {:margin-top "20vw" :text-align "center"}}
       "This is some text centered on the page."]
      [:div {:style {:margin-top (if (:long? @state) "200vh" "20vh")}}
       [:div {} [:button {:on-click #(swap! state assoc :large-modal? true)} "Open"]]
       [:div {} [:button {:on-click #(swap! state update :long? not)} "Toggle Page Size"]]
       [:div {} "Bottom of page"]]
      [modal/Container]])})

(defn render-application []
  (r/render
   (r/create-element App)
   (.. js/document (getElementById "app"))))

(render-application)
