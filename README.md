# ovs-crate

A Pallet crate to work with OpenVSwitch (OVS).

OVS is a great piece of software and a natural fit for connection
virtual machines together, and hence a good fit for use with pallet.

For the moment ovs-crate assumes OVS is always being set up on
Ubuntu 12.X hosts. This restriction can loosened in the future if
other people provide variations that works on other distributions
and versions.

## Getting started

For your lein2 project simply add the following dependency to your project.clj:

    [boardintelligence/ovs-crate 0.1.0-SNAPSHOT]

## Useage

The crate provides a server-spec **ovs-crate.specs/ovs-server** for which the **:configure**
phase does the following:
* Installs the required packages
* Sets up any number of OVS bridges as specified by config in the lift :environment
* Optionally makes the server a NAT/forwarder for various OVS bridges

Apart from the **:configure** phase there is one more function (and phase) that can be used
for administrating OVS. This is **ovs-crate.api/recreate-all-gre-connections** which is
used to just perform the GRE connection setup (since this may need updating as more
OVS bridges are added to the VLAN).

Details can be found below in their respective sections.

### Configuration for the :configure phase of ovs-crate.specs/ovs-server

The **:configure** phase of the ovs-server spec assumes that the environment passed
to pallet's lift operation looks like this:

    {:host-config
     {"host.to.configure"
      {:ovs-info {:interfaces-file (utils/resource-path "etc-network-interfaces") ;; the /etc/network/intefaces file to use for host
                  :bridges [{:name "private-br0" ;; the name of the OVS bridge
                             :host-interfaces-to-connect ["priv0"] ;; a vector of interfaces on the host to connect to bridge
                             :gre-connections [{:remote-ip "5.5.5.5"  ;; the remote ip of the remote bridge to connect
                                                :psk "the secret key" ;; the preshared key to use for connection
                                               }]
                             :act-as-forwarder {:from "priv0
                                                :via "eth0"
                                                :source "10.20.20.0/255.255.255.0"} ;; host is setup to act as NAT/forwarder from priv0 to eth0
                            }]}}}}

(utils/resource-path is a utility function provided by pallet to look for a specific relative path
on the classpath)

We opt to let the user provide his own complete */etc/network/intefaces* file (per host)
to allow for the greatest flexibility. Note that both bridges and gre-connections are
vectors allowing for several bridges and several gre-connections per bridge.

### Connecing OVS bridges on different hosts into one big VLAN

The GRE connections specified are created during :configure, but if you then add
more connections you may not want to run through the whole configure phase but instead
just update the GRE connections. The phase named **:recreate-all-gre-connections** does
just this and exists for the **ovs-crate.specs/ovs-server** server spec.

## License

Copyright © 2013 Board Intelligence

Distributed under the MIT License, see
[http://boardintelligence.mit-license.org](http://boardintelligence.mit-license.org)
for details.
