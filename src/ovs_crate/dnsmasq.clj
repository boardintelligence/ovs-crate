(ns ovs-crate.dnsmasq
  "Crate with functions for setting up and configuring dnsmasq"
  (:require [pallet.actions :as actions]
            [pallet.crate :as crate]
            [pallet.environment :as env]
            [pallet.utils :as utils]
            [pallet.crate :refer [defplan]]))

(defn- is-dhcp-ip?
  "Is IP to be assinged by DHCP?"
  [config]
  ;; a static IP has both a MAC and an IP given, so dhcp is the opposite of that
  (not (or (nil? (:ip config))
           (nil? (:mac config)))))

(defn- dhcp-hosts-content
  [hosts-config static-entries]
  (let [non-static (reduce str "" (map (fn [[host config]] (format "%s,%s,%s,infinite\n"
                                                                  (:mac config)
                                                                  (or (:private-ip config) (:ip config))
                                                                  (or (:private-hostname config) host)))
                                       hosts-config))]
    ;; add the static entries
    (reduce str non-static (map #(format "%s,%s,%s,infinite\n"
                                         (:mac %)
                                         (:ip %)
                                         (:hostname %))
                                static-entries))))

(defn- opts-file
  [service-name]
  (format "/etc/%s.opts" service-name))

(defn- hosts-file
  [service-name]
  (format "/etc/%s.hosts" service-name))

(defn- pid-file
  [service-name]
  (format "/var/run/%s.pid" service-name))

(defplan refresh-config
  "Refresh dnsmasq config."
  []
  (let [node-hostname (crate/target-name)
        hosts-file-content (dhcp-hosts-content
                            (env/get-environment [:host-config])
                            (env/get-environment [:host-config node-hostname :dnsmasq :static-entries]))
        local-opts-file (env/get-environment [:host-config node-hostname :dnsmasq :opts-file])
        service-name (env/get-environment [:host-config node-hostname :dnsmasq :service-name])]

    (actions/remote-file (opts-file service-name)
                         :local-file local-opts-file
                         :literal true)
    (actions/remote-file (hosts-file service-name)
                         :content hosts-file-content
                         :mode "0644"
                         :literal true)

    (actions/exec-checked-script
     "restart dnsmasq"
     ("service" ~service-name "restart"))))

(defplan install-upstart-job
  []
  (let [node-hostname (crate/target-name)
        interface (env/get-environment [:host-config node-hostname :dnsmasq :interface])
        range-start (env/get-environment [:host-config node-hostname :dnsmasq :range-start])
        domain (env/get-environment [:host-config node-hostname :dnsmasq :domain])
        service-name (env/get-environment [:host-config node-hostname :dnsmasq :service-name])
        local-opts-file (env/get-environment [:host-config node-hostname :dnsmasq :opts-file])]

    (actions/remote-file (format "/etc/init/%s.conf" service-name)
                         :mode "0644"
                         :literal true
                         :template "dnsmasq/dnsmasq.conf.template"
                         :values {:interface interface
                                  :range-start range-start
                                  :domain domain
                                  :opts-file (opts-file service-name)
                                  :hosts-file (hosts-file service-name)
                                  :pid-file (pid-file service-name)})
    (actions/remote-file (opts-file service-name)
                         :mode "0644"
                         :local-file local-opts-file
                         :literal true)

    (actions/exec-checked-script
     "Start dnsmasq for private network"
     (if-not (= @(pipe ("status" ~service-name)
                   ("grep running")) "")
       ("stop" ~service-name))
     ("start" ~service-name))))

(defplan setup-dnsmasq
  "Install dnsmasq, remove default config and refresh with out our config"
  []
  (actions/package "dnsmasq")
  (actions/exec-checked-script
   "Do not start default dnsmasq"
   ("/etc/init.d/dnsmasq stop")
   ("update-rc.d -f dnsmasq remove"))
  (install-upstart-job)
  (refresh-config))
