HALSpider
=========

Walks HAL-Json format and attempts to exhaust all relations without visting any link more then once and report back the status of those calls

Supports depth limiting and authorization using the auth header.

Obviously only explores the RESTful API using GET requests.

Example Usage:
=========
Help:
  - groovy HALSpider.groovy -h

Explore to a depth of 3:
  - groovy HALSpider.groovy -d 3 http://haltalk.herokuapp.com/

Explore to a depth of 3 and send Authorization header:
  - groovy HALSpider.groovy -d 3 -a 'Basic: test' http://haltalk.herokuapp.com/
