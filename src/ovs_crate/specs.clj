(ns ovs-crate.specs
  "Server and group specs for working with OVS"
  (:require [ovs-crate.ovs :as ovs]
            [pallet.api :as api]))

(def
  ^{:doc "Server spec for a server running 1 or more OVS bridges."}
  ovs-server
  (api/server-spec
   :phases
   {:configure (api/plan-fn (ovs/setup-ovs))
    :reboot (api/plan-fn (ovs/reboot))
    :recreate-all-gre-connections (api/plan-fn (ovs/recreate-all-gre-connections))}))
