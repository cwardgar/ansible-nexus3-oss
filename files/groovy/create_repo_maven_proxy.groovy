import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

configuration = new Configuration(
        repositoryName: parsed_args.name,
        recipeName: 'maven2-proxy',
        online: true,
        
        // All of the configuration below is associated with subclasses of org.sonatype.nexus.repository.Facet.
        // There are many more Facets than the 5 below, and it's hard to know for sure which Facets are associated
        // with Maven Proxy Repository creation without a debugger. However, if we look at the associated page in
        // the Admin GUI, and the options that are available there, these 5 seem to cover everything.
        attributes: [
                // See org.sonatype.nexus.repository.maven.internal.MavenFacetImpl.Config
                maven  : [
                        versionPolicy: parsed_args.version_policy.toUpperCase(),
                        layoutPolicy : parsed_args.layout_policy.toUpperCase()
                ],
                // See org.sonatype.nexus.repository.proxy.ProxyFacetSupport.Config
                proxy  : [
                        remoteUrl: parsed_args.remote_url,
                        // How long (in minutes) to cache artifacts before rechecking the remote repository.
                        // Release repositories should use -1.
                        contentMaxAge: parsed_args.version_policy.toUpperCase() == 'RELEASE' ? -1.0 : 1440.0,
                        metadataMaxAge: 1440.0
                ],
                // See org.sonatype.nexus.repository.storage.StorageFacetImpl.Config
                storage: [
                        blobStoreName: parsed_args.blob_store,
                        // 'writePolicy' only applies to hosted repos.
                        strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                ],
                // See org.sonatype.nexus.repository.cache.internal.NegativeCacheFacetImpl.Config
                negativeCache: [
                        enabled: true,
                        timeToLive: 1440.0
                ],
                // See org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config
                httpclient: [
                        blocked: false,
                        autoBlock: true,
                        authentication: authentication(),
                        // See org.sonatype.nexus.httpclient.config.ConnectionConfiguration. All fields are Nullable.
                        connection: [
                                timeout: null,
                                maximumRetries: null,
                                userAgentSuffix: null,
                                useTrustStore: null,
                                enableCircularRedirects: null,
                                enableCookies: null
                        ]
                ]
        ]
)

// See org.sonatype.nexus.httpclient.config.AuthenticationConfiguration and its subclasses.
def authentication() {
    if (parsed_args.type) {
        // This config is common to both org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
        // and org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration
        def ret = [
                type: parsed_args.type,
                username: parsed_args.remote_username,
                password: parsed_args.remote_password
        ]
        
        // This config is NtlmAuthenticationConfiguration-only.
        if (parsed_args.type == 'ntlm') {
            ret.host = null
            ret.domain = null
        }
        
        ret
    } else {
        // org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config.authentication can be null.
        null
    }
}

def existingRepository = repository.getRepositoryManager().get(parsed_args.name)

if (existingRepository != null) {
    existingRepository.stop()
    configuration.attributes['storage']['blobStoreName'] =
            existingRepository.configuration.attributes['storage']['blobStoreName']
    existingRepository.update(configuration)
    existingRepository.start()
} else {
    repository.getRepositoryManager().create(configuration)
}
