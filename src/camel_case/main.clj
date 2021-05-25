(ns camel_case.main
  {:author ["David Harrigan"]}
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [muuntaja.core :as m]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(def ^:private ->camelCase
  {:name ::camelCase
   :wrap (fn [handler]
           (fn [request]
             (cske/transform-keys csk/->camelCase (handler request))))})

(def app
  (ring/ring-handler
   (ring/router
    ["/" {:middleware [->camelCase]
          :get {:handler (fn [request]
                           (let [{{{:keys [msg]} :query} :parameters} request]
                             {:status 200 :body {:foo-bar-baz (str msg " GET")}}))
                :parameters {:query [:map
                                     {:closed true}
                                     [:msg {:optional true} string?]]}}
          :post {:handler (fn [request]
                            (let [{{{:keys [msg]} :body} :parameters} request]
                              {:status 201 :body {:foo-bar-baz (str msg " POST")}}))
                 :parameters {:body [:map
                                     {:closed true}
                                     [:msg string?]]}}}]
    {:data {:coercion rcm/coercion
            :muuntaja m/instance
            :middleware [muuntaja/format-middleware
                         parameters/parameters-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})))

(defn jetty-start
  []
  (jetty/run-jetty #'app {:port 8080 :join? false}))

(defn jetty-stop
  [^Server server]
  (.stop server)
  (.join server))

(comment

 (def server (jetty-start))

 (jetty-stop server)

 ;; Using `httpie` as my client...

 ;; GET EXAMPLE

 ;; http :8080 msg==hello
 ;;
 ;; HTTP/1.1 200 OK
 ;; Content-Length: 21
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Tue, 25 May 2021 09:58:24 GMT
 ;; Server: Jetty(9.4.40.v20210413)

 ;; {
 ;;     "fooBarBaz": "hello GET"
 ;; }

 ;; POST EXAMPLE

 ;; http POST :8080 msg=hello

 ;; HTTP/1.1 201 Created
 ;; Content-Length: 21
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Tue, 25 May 2021 09:58:58 GMT
 ;; Server: Jetty(9.4.40.v20210413)

 ;; {
 ;;     "fooBarBaz": "hello POST"
 ;; }

 ,)
