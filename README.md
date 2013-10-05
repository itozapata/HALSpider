HALSpider
=========

Walks HAL-Json format and attempts to exhaust all relations and report back the status of those calls

Supports depth limiting and authorization using the auth header.

Example Usage:
=========
Help:
groovy HALSpider.groovy -h

Explore to a depth of 3
groovy HALSpider.groovy -d 3 http://haltalk.herokuapp.com/

Explore to a depth of 3 and send Authorization header
groovy HALSpider.groovy -d 3 -a 'Basic: test' http://haltalk.herokuapp.com/
