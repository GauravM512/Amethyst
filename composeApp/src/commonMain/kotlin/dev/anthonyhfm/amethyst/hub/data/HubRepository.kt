package dev.anthonyhfm.amethyst.hub.data

class HubRepository(
    baseUrl: String = HubApiClient.DEFAULT_BASE_URL,
    bearerToken: String? = null,
) {
    val client = HubApiClient(baseUrl = baseUrl, bearerToken = bearerToken)

    val browseProjects = BrowseProjectsUseCase(client)
    val downloadPackage = DownloadProjectPackageUseCase(client)

    val getArtist = GetArtistUseCase(client)
    val getArtistProjects = GetArtistProjectsUseCase(client)
    val getPublishedProject = GetPublishedProjectUseCase(client)

    val getOwnedProjects = GetOwnedProjectsUseCase(client)
    val getOwnedProject = GetOwnedProjectUseCase(client)
    val createProject = CreateProjectUseCase(client)
    val updateProject = UpdateProjectUseCase(client)
    val publishProject = PublishProjectUseCase(client)
    val unpublishProject = UnpublishProjectUseCase(client)
    val deleteProject = DeleteProjectUseCase(client)
    val uploadPackage = UploadProjectPackageUseCase(client)
    val uploadThumbnail = UploadProjectThumbnailUseCase(client)
    val deleteThumbnail = DeleteProjectThumbnailUseCase(client)

    val followArtist = FollowArtistUseCase(client)
    val unfollowArtist = UnfollowArtistUseCase(client)
}
