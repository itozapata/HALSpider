import org.apache.commons.httpclient.HostConfiguration
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Created with IntelliJ IDEA.
 * User: izapata
 * Date: 10/3/13
 * Time: 4:07 PM
 *
 * Give it a rest endpoint and a depth and it will spider through the site following the relations till it exhausts
 * the relations to follow or the depth is reached.
 */

@GrabConfig(systemClassLoader=true)
@Grab('org.json:json:20090211')
@Grab('commons-httpclient:commons-httpclient:3.1')
@Grab('commons-io:commons-io:2.4')

CliBuilder cli = new CliBuilder(usage: 'groovy com.videocritic.test.RestExplorer.groovy -[da] "server"')
cli.d(longOpt: 'depth', 'max relation depth to resolve', required: false, args: 1)
cli.s(longOpt: 'server', 'index resource uri', required: false)
cli.a(longOpt: 'auth', 'Authorization header', required: false, args: 1)
cli.h(longOpt: 'help', 'Help menu', required: false)

def options = cli.parse(this.args)

if(options.h ||
    (options.arguments().size() == 0 &&
        !options.d &&
        !options.s &&
        !options.a)) {

    cli.usage()
    return
}

def homeLink = options.arguments().size() > 0 ? options.arguments().get(0) : 'http://haltalk.herokuapp.com/'
def depth = options.d ? Integer.parseInt(options.d) : 1
def authorization = options.a ? options.a : null

def visitedLinks = [:]
def errorLinks = [:]
HttpClient client = new HttpClient()

println "Contacting: ${homeLink}"
println "  To a recursive depth of: ${depth}"
if(authorization != null) {
    println "  With Authorization: ${authorization}"
}
println ""

setupRequestHeaders = {GetMethod method ->
    if(authorization != null) {
        method.setRequestHeader('Authorization', authorization)
    }

    method.setRequestHeader('Accept', 'application/json')
    //todo: encoding doesnt work probably need to set up a setting on httpClient or something
    //method.setRequestHeader('Accept-Encoding', 'gzip,deflate,sdch')
}

visitLink = {String uri ->

    String url = uri;
    if(!url.startsWith("http://")) {
        url = "${homeLink}${uri}"
        // little funky and should probably switch to using a url so i could do relative resolving easier
        url = url.replaceAll("//", "/")
        url = url.replaceFirst("/", "//")
    }

    if(visitedLinks[url] == null) {

        GetMethod method = new GetMethod(url)
        try{
            setupRequestHeaders(method)

            int responseCode = client.executeMethod(method)

            visitedLinks[url] = responseCode

            if(responseCode >= 200 && responseCode < 300) {
                InputStream inputStream = method.getResponseBodyAsStream()

                return new JSONObject(IOUtils.toString(inputStream))
            }
            else {
                errorLinks[url] = responseCode
            }
        }
        finally {
            method.releaseConnection()
        }
    }

    return null
}

resolveAllRelations = {def resource, int currentDepth ->
    if(currentDepth >= depth)
        return

    if(resource instanceof JSONArray) {
        resource.myArrayList.each { def object ->
            resolveAllRelations.call(object, currentDepth+1)
        }
    }
    else if(resource instanceof JSONObject) {
        JSONObject jsonObject = resource

        if(jsonObject.has('_embedded')) {
            JSONObject embeddedRoot = jsonObject.get('_embedded')
            embeddedRoot.keys().each { String relation ->
                try {
                    resolveAllRelations.call(embeddedRoot.get(relation), currentDepth+1)
                }
                catch(Exception e) {
                    println e
                }
            }
        }

        // all resources should have a links section but this keeps us safe from some stuff.
        if(jsonObject.has('_links')) {

            JSONObject linksRoot = jsonObject.get('_links')
            linksRoot.keys().each { String relation ->

                //TODO: doesnt support curries right now since there is no input to fill them in.
                if(relation.equalsIgnoreCase('curies')
                    || linksRoot.getJSONObject(relation).has('templated')){

                    return
                }

                try {
                    String href = linksRoot.getJSONObject(relation).getString('href')

                    JSONObject ret = visitLink.call(href)
                    if(ret != null) {
                        resolveAllRelations.call(ret, currentDepth+1)
                    }
                }
                catch(Exception e) {
                    println e
                }
            }
        }
    }
}


// get the home resource
JSONObject homeResource = visitLink.call(homeLink)
resolveAllRelations.call(homeResource, 1)

println 'Visited links: (responseCode, url)'
visitedLinks.each {key, value -> println "${value}, ${key}"}

if(!errorLinks.isEmpty()) {
    println ''
    println 'Problem links: (responseCode, url)'
    errorLinks.each {key, value -> println "${value}, ${key}"}

    System.exit(1)
}

println ''


