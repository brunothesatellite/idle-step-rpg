Add-Type -AssemblyName System.Drawing

$root = "D:\VS_Code_Workspaces\idle-step-rpg"
$outDirs = @("$root\assets\item", "$root\android\app\src\main\assets\item")
foreach ($d in $outDirs) { New-Item -ItemType Directory -Force $d | Out-Null }

function Rect($g, $hex, $x, $y, $w, $h) {
    $c = [Drawing.ColorTranslator]::FromHtml($hex)
    $b = New-Object Drawing.SolidBrush($c)
    $g.FillRectangle($b, [int]$x, [int]$y, [int]$w, [int]$h)
    $b.Dispose()
}

function NewIcon($name, [scriptblock]$draw) {
    $bmp = New-Object Drawing.Bitmap 32, 32
    $g = [Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'None'
    $g.InterpolationMode = 'NearestNeighbor'
    $g.PixelOffsetMode = 'Half'
    $g.Clear([Drawing.Color]::Transparent)
    & $draw $g
    $g.Dispose()
    foreach ($d in $outDirs) { $bmp.Save("$d\$name.png", [Drawing.Imaging.ImageFormat]::Png) }
    $bmp.Dispose()
}

function Bottle {
    param($g, $liquid, $glow, $cork = "#6B4A2A")
    Rect $g "#140D0D" 11 10 10 2
    Rect $g "#140D0D" 9 12 14 12
    Rect $g "#140D0D" 11 24 10 2
    Rect $g "#140D0D" 13 26 6 1
    Rect $g "#D9D3C8" 12 11 8 1
    Rect $g "#D9D3C8" 10 13 12 10
    Rect $g "#D9D3C8" 12 23 8 2
    Rect $g $liquid 11 16 10 6
    Rect $g $liquid 12 22 8 1
    Rect $g $glow 12 17 2 3
    Rect $g "#FFFFFF" 11 14 2 4
    Rect $g "#140D0D" 13 7 6 4
    Rect $g "#D9D3C8" 14 8 4 3
    Rect $g $cork 13 4 6 3
    Rect $g "#3A2414" 13 7 6 1
}

NewIcon "cons_potion_red" { param($g) Bottle $g "#C8323C" "#F06A6A" }
NewIcon "cons_potion_purple" { param($g) Bottle $g "#7B3FC4" "#C08CFF" }
NewIcon "cons_elixir" {
    param($g)
    Rect $g "#0D1218" 12 6 8 3
    Rect $g "#0D1218" 10 9 12 17
    Rect $g "#0D1218" 12 26 8 2
    Rect $g "#C9E6FF" 13 7 6 2
    Rect $g "#C9E6FF" 11 10 10 14
    Rect $g "#C9E6FF" 13 24 6 2
    Rect $g "#2E8BFF" 12 15 8 8
    Rect $g "#7CC4FF" 13 16 2 4
    Rect $g "#FFFFFF" 12 12 2 3
    Rect $g "#6B4A2A" 13 3 6 3
}

NewIcon "cons_herb" {
    param($g)
    Rect $g "#123A1C" 15 9 2 16
    Rect $g "#123A1C" 12 13 2 12
    Rect $g "#123A1C" 18 12 2 13
    Rect $g "#3E9E4A" 10 11 5 2
    Rect $g "#3E9E4A" 17 9 5 2
    Rect $g "#3E9E4A" 13 8 2 4
    Rect $g "#3E9E4A" 18 15 4 2
    Rect $g "#6FCB72" 11 10 2 1
    Rect $g "#6FCB72" 18 8 2 1
    Rect $g "#6B4A2A" 12 22 8 2
    Rect $g "#3A2414" 12 24 8 1
}

function Mushroom {
    param($g, $cap, $spot, $glow)
    Rect $g "#241414" 10 9 12 1
    Rect $g "#241414" 8 10 16 1
    Rect $g "#241414" 7 11 18 5
    Rect $g "#241414" 8 16 16 1
    Rect $g $cap 11 10 10 1
    Rect $g $cap 9 11 14 4
    Rect $g $glow 10 12 3 1
    Rect $g $glow 17 13 3 1
    Rect $g $spot 13 11 2 2
    Rect $g $spot 18 12 2 2
    Rect $g "#241414" 13 16 6 10
    Rect $g "#D8C9A4" 14 17 4 8
    Rect $g "#A8946B" 14 24 4 2
}

NewIcon "cons_mushroom" { param($g) Mushroom $g "#D9822B" "#F5C16C" "#F5C16C" }
NewIcon "cons_mushroom_red" { param($g) Mushroom $g "#C8323C" "#F2C6A0" "#F06A6A" }

NewIcon "cons_shield" {
    param($g)
    $rows = @(
        @(9,7,14), @(7,8,18), @(7,9,18), @(7,10,18), @(7,11,18), @(7,12,18),
        @(7,13,18), @(8,14,16), @(8,15,16), @(9,16,14), @(10,17,12),
        @(11,18,10), @(12,19,8), @(13,20,6), @(14,21,4), @(15,22,2)
    )
    foreach ($r in $rows) { Rect $g "#241414" $r[0] $r[1] $r[2] 1 }
    foreach ($r in $rows) {
        if ($r[2] -gt 4) { Rect $g "#7A5A3A" ($r[0]+1) $r[1] ($r[2]-2) 1 }
    }
    Rect $g "#C9A86B" 12 11 8 4
    Rect $g "#F5D98A" 14 12 4 2
    Rect $g "#3A2414" 15 15 2 4
}

NewIcon "cons_lightning" {
    param($g)
    $pts = @(
        (New-Object Drawing.Point 19,3),
        (New-Object Drawing.Point 10,17),
        (New-Object Drawing.Point 15,17),
        (New-Object Drawing.Point 11,29),
        (New-Object Drawing.Point 22,14),
        (New-Object Drawing.Point 16,14),
        (New-Object Drawing.Point 22,3)
    )
    $b = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml("#F5C542"))
    $g.FillPolygon($b, $pts)
    $b.Dispose()
    $p = New-Object Drawing.Pen([Drawing.ColorTranslator]::FromHtml("#A86A12"), 1)
    $g.DrawPolygon($p, $pts)
    $p.Dispose()
    Rect $g "#FFF3A0" 16 5 2 6
}

NewIcon "cons_bomb" {
    param($g)
    Rect $g "#141010" 8 12 16 15
    Rect $g "#141010" 10 10 12 2
    Rect $g "#141010" 10 27 12 1
    Rect $g "#2A2A32" 10 14 12 10
    Rect $g "#5A5A68" 11 15 4 4
    Rect $g "#0B0B0F" 13 11 6 3
    $p = New-Object Drawing.Pen([Drawing.ColorTranslator]::FromHtml("#8A6A3A"), 2)
    $g.DrawLine($p, 20, 11, 24, 6)
    $p.Dispose()
    $b = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml("#FFD45A"))
    $g.FillEllipse($b, 23, 4, 3, 3)
    $b.Dispose()
    $b = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml("#FF7A2A"))
    $g.FillEllipse($b, 24, 5, 2, 2)
    $b.Dispose()
}

NewIcon "cons_scroll" {
    param($g)
    Rect $g "#241414" 8 7 16 19
    Rect $g "#D8C9A4" 9 8 14 17
    Rect $g "#B8A47A" 8 6 16 2
    Rect $g "#B8A47A" 8 25 16 2
    Rect $g "#8A6A3A" 9 9 12 1
    Rect $g "#8A6A3A" 10 12 10 1
    Rect $g "#8A6A3A" 10 15 8 1
    Rect $g "#8A6A3A" 10 18 11 1
    Rect $g "#C8323C" 15 8 2 17
    Rect $g "#F06A6A" 15 8 1 17
}

Write-Output "icônes de consommables générées"

