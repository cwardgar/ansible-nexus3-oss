import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

configuration = new Configuration(
        repositoryName: parsed_args.name,
        recipeName: 'maven2-group',
        online: true,
        
        attributes: [
                // See org.sonatype.nexus.repository.group.GroupFacetImpl.Config
                group  : [
                        memberNames: parsed_args.member_repos
                ],
                // See org.sonatype.nexus.repository.storage.StorageFacetImpl.Config
                storage: [
                        blobStoreName: parsed_args.blob_store,
                        // 'writePolicy' only applies to hosted repos.
                        strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                ]
        ]
)

def existingRepository = repository.getRepositoryManager().get(parsed_args.name)

if (existingRepository != null) {
    existingRepository.stop()
    configuration.attributes['storage']['blobStoreName'] = existingRepository.configuration.attributes['storage']['blobStoreName']
    existingRepository.update(configuration)
    existingRepository.start()
} else {
    repository.getRepositoryManager().create(configuration)
}
