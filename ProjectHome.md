Publiy is a distributed content-based publish/subscribe system that is originally developed at the University of Toronto. Publiy supports five modes of operation:

  * **Byzantine-fault-tolerance (PubliyPrime):** Byzantine behavior of publish/subscribe brokers may be rooted in malicious intent of their operators, software bugs or hardware defects. These may lead to disruption of publications flowing from publishers to subscribers, such as censorship, delays, tampering, re-ordering, etc. PubliyPrime features capabilities that allows correct brokers to monitor neighboring brokers, detect potential their misbehaviors and take action in order to ensure that the service integrity is preserved.

  * **Fault-tolerance and reliability (Publiy):** This mode ensure loss-free delivery of publications despite crash or partitioning of publish/subscribe brokers.

  * **Opportunistic and multi-path publication forwarding (PubliyMP):** Design and maintenance of the broker overlay in content-based publish/subscribe is inherently challenging due to the diversity of publication traffic patterns that range from unicast to multicast and broadcast. PubliyMP relies on the diversity of end-to-end paths in a specially constructed high connectivity overlay mesh to drastically improve the efficiency of the publication dissemination process by avoiding the so called _pure-forwarding_ brokers.

  * **Bulk content dissemination (Publiy+):** Bulk content publications might be tens or hundreds of megabytes in size, i.e., much larger than conventional event publications which typically were only a few thousand kilo-bytes. In this mode, Publiy

  * **Normal operation:** This is the most efficient mode of operation which delivers the highest throughput.