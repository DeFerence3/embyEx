<#
.SYNOPSIS
    Installs the latest EmbyEx desktop release on Windows, for the current user (no admin needed).

.EXAMPLE
    irm https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.ps1 | iex

.EXAMPLE
    # Newest release, pre-releases (vX.Y.Z-beta.N) included:
    & ([scriptblock]::Create((irm https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.ps1))) -Beta

.NOTES
    Setting $env:EMBYEX_BETA = '1' before the first example also installs pre-releases.
    Works on Windows PowerShell 5.1 and PowerShell 7.
#>
param(
    [switch]$Beta
)

$ErrorActionPreference = 'Stop'
# Invoke-WebRequest renders progress very slowly on Windows PowerShell 5.1.
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

function Install-EmbyEx {
    param(
        [bool]$IncludePrerelease
    )

    $api = 'https://api.github.com/repos/DeFerence3/embyEx/releases'
    $headers = @{
        'Accept' = 'application/vnd.github+json'
        'User-Agent' = 'embyex-installer'
    }

    if ($IncludePrerelease) {
        # Highest MAJOR.MINOR.PATCH wins; a beta orders before the release it precedes.
        $release = Invoke-RestMethod -Uri "${api}?per_page=30" -Headers $headers |
            Where-Object { -not $_.draft -and $_.tag_name -match '^v(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$' } |
            ForEach-Object {
                $null = $_.tag_name -match '^v(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$'
                $betaNumber = [int]::MaxValue
                if ($Matches[4]) { $betaNumber = [int]$Matches[4] }
                [pscustomobject]@{
                    Release = $_
                    Major = [int]$Matches[1]
                    Minor = [int]$Matches[2]
                    Patch = [int]$Matches[3]
                    BetaNumber = $betaNumber
                }
            } |
            Sort-Object -Property Major, Minor, Patch, BetaNumber -Descending |
            Select-Object -First 1 |
            ForEach-Object { $_.Release }
        if (-not $release) { throw 'No published releases were found.' }
    } else {
        $release = Invoke-RestMethod -Uri "$api/latest" -Headers $headers
    }

    $asset = $release.assets | Where-Object { $_.name -like '*.msi' } | Select-Object -First 1
    if (-not $asset) { throw "Release $($release.tag_name) has no Windows installer." }

    $installer = Join-Path -Path ([IO.Path]::GetTempPath()) -ChildPath $asset.name
    try {
        Write-Host "Downloading EmbyEx $($release.tag_name)..."
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $installer -UseBasicParsing -Headers @{ 'User-Agent' = 'embyex-installer' }

        Write-Host 'Installing...'
        $process = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', "`"$installer`"", '/passive', '/norestart') -Wait -PassThru
        # 3010: installed, a restart is required to finish.
        if ($process.ExitCode -ne 0 -and $process.ExitCode -ne 3010) {
            throw "The installer failed with exit code $($process.ExitCode)."
        }
    } finally {
        Remove-Item -Path $installer -Force -ErrorAction SilentlyContinue
    }

    Write-Host "EmbyEx $($release.tag_name) is installed. Launch it from the Start menu."
}

Install-EmbyEx -IncludePrerelease ($Beta -or $env:EMBYEX_BETA -eq '1')
