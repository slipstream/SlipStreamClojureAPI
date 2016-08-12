# Legacy REST API

SlipStream Clojure client library to query the legacy parts of the
[SlipStream server API][ss-api].  This currently covers all of the
cloud application and management functions.

## Usage

### Query SlipStream Run

Library looks for the configuration file named `slipstream.context` in
the following places and in defined the order

```
1. in the resources directory defined for the JVM
2. current directory
3. in user home directory
4. /opt/slipstream/client/bin/
5. /opt/slipstream/client/sbin/
6. system temporary directory

```

Here is an example of `slipstream.context`

```
[contextualization]
diid = <uuid-of-slipstream-run>
cookie = com.sixsq.slipstream.cookie=<cookie-content> Path:/
serviceurl = https://nuv.la
node_instance_name = orchestrator-exoscale-ch-gva
```

Require the run namespace and use the function to interact with the
run

```clojure
(require '[sixsq.slipstream.client.api.run :as r])

(r/get-state)
(r/action-scale-up "web-server" 3)
(r/terminate)
```

### Contacting server in insecure mode

For contacting server in insecure mode (i.e. w/o checking authenticity of the
remote server), provide `:insecure? true` in `req` map in
**sixsq.slipstream.client.api** namespaces.  Alternatively, re-set
the root of authentication context with

```clojure
(require '[sixsq.slipstream.client.api.authn :as a])
(a/set-context! {:insecure? true})
(deploy ..)
```

or wrap the API call for the local rebinding of the authentication context as
follows

```clojure
(a/with-context {:insecure? true} (deploy ..))
```

[ss-api]: http://ssapi.sixsq.com/
