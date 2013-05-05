(ns ovs-crate.racoon
  "Crate with functions for setting up and configuring racoon (ipsec)"
  (:require [pallet.actions :as actions]
            [pallet.crate :as crate]
            [pallet.environment :as env]
            [pallet.utils :as utils]
            [pallet.crate :refer [defplan]]))

(defn- generate-psk-content
  [psks]
  (clojure.string/join "\n" (map (fn [[peer psk]] (format "%s\t%s" peer psk)) psks)))

(defplan setup-racoon
  "Remove ovs-ipsec and install our own custom racoon setup"
  []
  (let [hostname (crate/target-name)
        racoon-config (env/get-environment [:host-config hostname :racoon] nil)]
    (when-not (nil? racoon-config)
      (actions/package "ipsec-tools" :action :install)
      (actions/package "racoon" :action :install)
      (actions/remote-file "/etc/racoon/racoon.conf-manual"
                           :local-file (:config-file racoon-config)
                           :literal true)
      (actions/remote-file "/etc/racoon/psk.txt-manual"
                           :content (generate-psk-content (:psks racoon-config))
                           :literal true
                           :mode "0600")
      (actions/remote-file "/etc/init.d/racoon"
                           :local-file (utils/resource-path "ipsec/init-d-racoon")
                           :literal true
                           :mode "0755")
      (actions/exec-checked-script
       "Restart racoon"
       ("service racoon restart")))))
