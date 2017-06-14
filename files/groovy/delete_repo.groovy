import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

existingRepository = repository.getRepositoryManager().get(parsed_args.name)
if (existingRepository != null) {
    repository.getRepositoryManager().delete(parsed_args.name)
}
