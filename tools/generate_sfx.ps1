Add-Type -AssemblyName System.Drawing

$root = "D:\VS_Code_Workspaces\idle-step-rpg"
$outDirs = @("$root\assets\audio", "$root\android\app\src\main\assets\audio")
foreach ($d in $outDirs) { New-Item -ItemType Directory -Force $d | Out-Null }

$rate = 22050
$script:seed = 7

function WriteWav($path, $samples) {
    $ms = New-Object IO.MemoryStream
    $bw = New-Object IO.BinaryWriter($ms)
    $bw.Write([Text.Encoding]::ASCII.GetBytes("RIFF"))
    $bw.Write([int](36 + $samples.Count * 2))
    $bw.Write([Text.Encoding]::ASCII.GetBytes("WAVEfmt "))
    $bw.Write([int]16)
    $bw.Write([int16]1)
    $bw.Write([int16]1)
    $bw.Write([int]$script:rate)
    $bw.Write([int]($script:rate * 2))
    $bw.Write([int16]2)
    $bw.Write([int16]16)
    $bw.Write([Text.Encoding]::ASCII.GetBytes("data"))
    $bw.Write([int]($samples.Count * 2))
    foreach ($s in $samples) {
        $v = [Math]::Max(-32767, [Math]::Min(32767, [int]($s * 32767)))
        $bw.Write([int16]$v)
    }
    $bw.Flush()
    [IO.File]::WriteAllBytes($path, $ms.ToArray())
    $bw.Dispose()
    $ms.Dispose()
}

function Note($freq, $ms, $vol, $wave = "square", $slideTo = 0) {
    $n = [int]($rate * $ms / 1000.0)
    $samples = New-Object 'System.Collections.Generic.List[double]'
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / [double]$n
        $f = if ($slideTo -gt 0) { $freq + ($slideTo - $freq) * $t } else { $freq }
        $phase += 2.0 * [Math]::PI * $f / $rate
        $raw = switch ($wave) {
            "triangle" { 2.0 / [Math]::PI * [Math]::Asin([Math]::Sin($phase)) }
            "sine" { [Math]::Sin($phase) }
            "noise" { ([Random]::new($script:seed + $i).NextDouble() * 2.0 - 1.0) }
            default { if ([Math]::Sin($phase) -ge 0) { 1.0 } else { -1.0 } }
        }
        $attack = [Math]::Min(1.0, $i / ($rate * 0.004))
        $release = [Math]::Min(1.0, ($n - $i) / ($rate * 0.015))
        $samples.Add($raw * $vol * $attack * $release)
    }
    return $samples
}

function Mix($a, $b) {
    $n = [Math]::Max($a.Count, $b.Count)
    $out = New-Object 'System.Collections.Generic.List[double]'
    for ($i = 0; $i -lt $n; $i++) {
        $va = if ($i -lt $a.Count) { $a[$i] } else { 0.0 }
        $vb = if ($i -lt $b.Count) { $b[$i] } else { 0.0 }
        $out.Add($va + $vb)
    }
    return $out
}

function ConcatSfx($notes) {
    $out = New-Object 'System.Collections.Generic.List[double]'
    foreach ($n in $notes) {
        foreach ($s in $n) { $out.Add($s) }
    }
    return $out
}

$ui = Note 880 55 0.22 "square"
$hitNoise = Note 220 70 0.22 "noise" 70
$hitTone = Note 170 80 0.22 "square" 90
$hit = Mix $hitNoise $hitTone
$critNotes = @(
    (Note 660 55 0.25 "square"),
    (Note 990 90 0.28 "square"),
    (Note 1320 60 0.20 "square")
)
$crit = ConcatSfx $critNotes
$hurt = Note 180 110 0.22 "square" 80
$levelNotes = @(
    (Note 523 75 0.20 "square"),
    (Note 659 75 0.20 "square"),
    (Note 784 75 0.22 "square"),
    (Note 1047 150 0.24 "square")
)
$level = ConcatSfx $levelNotes
$bossNotes = @(
    (Note 110 180 0.22 "square"),
    (Note 98 180 0.22 "square"),
    (Note 82 320 0.24 "square"),
    (Note 55 200 0.10 "noise")
)
$boss = ConcatSfx $bossNotes
$chestNotes = @(
    (Note 988 55 0.20 "square"),
    (Note 1319 80 0.22 "square"),
    (Note 1760 110 0.18 "square")
)
$chest = ConcatSfx $chestNotes
$consumeTone = Note 300 150 0.20 "triangle" 700
$consumeSpark = Note 900 90 0.08 "sine"
$consume = Mix $consumeTone $consumeSpark
$victoryNotes = @(
    (Note 523 105 0.20 "square"),
    (Note 659 105 0.20 "square"),
    (Note 784 105 0.22 "square"),
    (Note 1047 220 0.24 "square")
)
$victory = ConcatSfx $victoryNotes
$defeatNotes = @(
    (Note 392 130 0.20 "square"),
    (Note 330 140 0.20 "square"),
    (Note 262 160 0.22 "square"),
    (Note 196 260 0.22 "square")
)
$defeat = ConcatSfx $defeatNotes
$upgradeNoise = Note 180 45 0.18 "noise" 80
$upgradeNotes = @(
    (Note 620 55 0.22 "square"),
    (Note 930 90 0.18 "square")
)
$upgradeTone = ConcatSfx $upgradeNotes
$upgrade = Mix $upgradeNoise $upgradeTone

$set = @{
    "ui_tap" = $ui
    "hit" = $hit
    "crit" = $crit
    "hurt" = $hurt
    "level" = $level
    "boss" = $boss
    "chest" = $chest
    "consume" = $consume
    "victory" = $victory
    "defeat" = $defeat
    "upgrade" = $upgrade
}

foreach ($name in $set.Keys) {
    foreach ($d in $outDirs) { WriteWav "$d\$name.wav" $set[$name] }
}

$license = @"
Sons 8 bits de La Marche des Ames
Générés proceduralement pour ce projet (aucun asset externe).
Licence : CC0 1.0 (domaine public).
"@
foreach ($d in $outDirs) { Set-Content "$d\LICENSE.txt" $license -Encoding utf8 }

Write-Output "sons générés : $($set.Keys.Count)"

