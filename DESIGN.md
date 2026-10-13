# Chrynalogia UI reference

앞으로 UI 변경은 아래 Figma의 Chrynalogia 배치를 기준으로 하고, 색상·글자 크기·컴포넌트 비율은 사용자가 지정한 One UI 키트를 우선한다.

- [Chrynalogia 그리드 화면](https://www.figma.com/design/fS63AwMfT7fvMUuyiOGcEW/Chrynalogia?node-id=1-5)
- [선택한 커스텀 격자](https://www.figma.com/design/fS63AwMfT7fvMUuyiOGcEW/Chrynalogia?node-id=1-128)
- [라인 화면](https://www.figma.com/design/fS63AwMfT7fvMUuyiOGcEW/Chrynalogia?node-id=1-176)
- [카메라 세로 메뉴](https://www.figma.com/design/fS63AwMfT7fvMUuyiOGcEW/Chrynalogia?node-id=1-326)
- [사용자 지정 One UI Community 키트](https://www.figma.com/community/file/1456035621603784201/one-ui-design-kit)
- [읽은 키트의 색상](https://www.figma.com/design/vUfBjqkEaNgegMLjBuk7BG/One-UI-Design-Kit--Community-?node-id=10-209)
- [읽은 키트의 글자 크기](https://www.figma.com/design/vUfBjqkEaNgegMLjBuk7BG/One-UI-Design-Kit--Community-?node-id=10-205)
- [읽은 키트의 하단 탐색](https://www.figma.com/design/vUfBjqkEaNgegMLjBuk7BG/One-UI-Design-Kit--Community-?node-id=1495-10175)

## Layout

그리드·라인·설정의 3개 하단 탭을 유지한다. 그리드는 두 열 카드이며, 미리보기의 가로:세로는 원본 218:148을 따른다. 선택한 카드는 테두리로 표시하고 커스텀 직각 격자를 선택하면 해당 카드 아래에 가로·세로 칸 수 입력을 펼친다. 라인 화면은 색상 카드와 불투명도·두께·아웃라인 카드를 구분한다. 카메라 메뉴는 왼쪽의 좁은 세로 목록이다. Android 터치 영역은 작은 원본 목업의 픽셀 수를 그대로 축소하지 않고 사용할 수 있는 크기로 유지한다.

## Tokens

`Ui.java`에서 공유한다. 키트의 실제 디자인 컨텍스트에 있는 색상을 사용하며, 예시 라벨과 실제 fill 값이 다르면 실제 fill 값을 따른다.

| Token | Dark | Light |
| --- | --- | --- |
| Background | #010102 | #F1F1F3 |
| Container | #17171A | #FCFCFF |
| Foreground surface | #2E2E30 | #EAEAEA |
| Primary | #387AFF | #387AFF |
| Text | #FFFFFF | #000000 |

제목 36sp, 섹션 제목 24sp, 본문 18sp, 설명 14sp, 하단 탐색 라벨 10sp를 사용한다. 읽기 쉬운 대비를 위해 작은 설명 글자에는 더 명확한 보조색을 적용한다. 컴포넌트는 키트의 28dp 둥근 모서리와 22–24dp 아이콘을 따른다. 갤럭시 시스템 sans-serif 폰트를 사용하며 키트에 있는 폰트 파일을 별도로 재배포하지 않는다.

## Assets and behavior

원본 로고는 기존 `app_logo.png`를 그대로 재사용한다. 원본 디자인의 반복 삼각형 선은 그리드 데이터를 설명하는 샘플이며, 실제 앱은 선택한 그리드의 기하를 `GridView`로 미리 보여준다. 카메라 배경도 실제 촬영 미리보기다. 색상 선택기의 그라디언트·핸들은 실제 HSV 값에 따라 바뀌는 입력 UI다. 인앱 아이콘은 사용자의 최신 지시에 따라 키트 Icons 페이지의 SVG 원본을 그대로 포함하고 AndroidSVG로 렌더링한다. 기존 Material 아이콘은 제거한다. 카메라 메뉴를 여는 아이콘은 제공된 앱 로고다.

언어는 Android LocaleManager로 시스템 기본값·한국어·영어를 제공하고, 테마는 앱 전용 설정으로 시스템 기본값·밝게·어둡게를 제공한다. 아웃라인은 선 색의 선형 sRGB 휘도에 따라 흰색·검은색 중 대비가 더 높은 색을 선택한다. 단일 61.8% 황금비 항목은 목록에서 제거하고 기존 선택은 황금비 격자로 이전한다. 황금비 격자와 황금 나선은 유지한다.

## v0.12 component references

- Switches: kit `1493:8885`; original SVG states at `res/raw/kit_switch_on.svg` and `kit_switch_off.svg`, 40 × 22dp, 17dp white thumb. Native CompoundButton retains checked state and accessibility.
- Sliders: kit `1493:8572`, Regular style; 18dp pill track, blue fill, #848487 rail, 24dp black thumb and 2dp blue ring. Native SeekBar owns range, touch and accessibility.
- Containers Info: kit `547:3004`; settings groups use 10dp outer inset, 28dp corners, 20dp horizontal content padding, 18sp main text, 14sp information, 2dp text gap. Remove separate group headings.
- In-App Navigation: kit `1495:10175`; 14dp backdrop blur, 60% dark/light surface, 4dp inset, 46dp selected capsule, 22dp icons, 10sp labels. Capture only underlying app content into the background image and apply RenderEffect there; icons remain sharp.
- Expanded title area: [Samsung guide p28](https://www.design.samsung.com/kr/contents/one-ui/download/oneui_design_guide_kor.pdf#page=28), 39.76% of the phone content height. A different tab starts at the first function by scrolling past the header; pulling down reveals the expanded title. Compact camera settings omit the expanded area.
- Selected grid border is white. Custom rectangular grid count rows keep their measured space while invisible. No Layout panel in the Grid tab.
- Custom color is a radio choice with a saved custom value, six-digit HEX editor and trailing swatch; 20dp vertical row padding instead of the preset rows' 12dp.
- Camera logo occupies a fixed 36dp window. The separate 48dp grouped menu opens below it, or above when landscape height has insufficient space.
- Rabatment is generated in its original frame before rotation; the path is transformed to keep stroke width constant. Other grids keep their existing rotation geometry, preserving the golden spiral proportions and 45-degree diagonals. Imported SVG/bitmap rotation retains its existing aspect handling.

Original Icons page exports (`320:2`) are stored unmodified in `res/raw/kit_*.svg`: list_grid, color_adjustment, dark_outline, settings_outline, camera_outline, keyboard_language_nomal, delete_outline, add, close, eyes, password_hide, rotate_outline, flip outline and power. Switch states and add were downloaded using the URLs supplied by Figma design context; the remaining icons were exported with Figma's normal SVG export UI. The Figma MCP Starter call quota was exhausted; the browser editor remained available to read Containers Info and export icon originals.