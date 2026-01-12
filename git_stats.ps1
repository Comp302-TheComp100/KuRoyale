[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$log = git log --no-merges --numstat --format='%aN'
$stats = @{}
$currentAuthor = $null

foreach ($line in $log) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }

    # Match numstat lines: inserted deleted filepath
    if ($line -match '^(\d+|-)\s+(\d+|-)\s+(.*)$') {
        if ($null -ne $currentAuthor) {
            $added = $matches[1]
            $deleted = $matches[2]

            # Handle binary files
            if ($added -eq '-') { $added = 0 }
            if ($deleted -eq '-') { $deleted = 0 }

            if (-not $stats.ContainsKey($currentAuthor)) {
                $stats[$currentAuthor] = @{ Added = 0; Deleted = 0 }
            }

            $stats[$currentAuthor].Added += [int]$added
            $stats[$currentAuthor].Deleted += [int]$deleted
        }
    }
    else {
        # It's an author name
        $currentAuthor = $line.Trim()
    }
}

$stats.GetEnumerator() | 
    Select-Object @{N='Author';E={$_.Key}}, @{N='Added';E={$_.Value.Added}}, @{N='Deleted';E={$_.Value.Deleted}} | 
    Sort-Object Added -Descending | 
    Format-Table -AutoSize
