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
## v0.13 overrides

최신 사용자 요청에 따라 아래 내용이 v0.12의 해당 규칙보다 우선한다.

- 카메라 메뉴를 여는 로고는 `logo/circle_icon.svg`의 원본을 `res/raw/camera_circle.svg`에 그대로 복사해 색상 변경 없이 표시한다. 앱 첫 화면의 큰 로고는 기존 `app_logo.png`다.
- 카메라 로고의 터치 창은 48 × 36dp, 실제 SVG 뷰는 44dp다. SVG 자체의 투명 여백을 포함한다. 왼쪽 여백은 삼성 상단 버튼 목록의 오른쪽 여백과 맞추며, 세로 그룹 메뉴는 동일한 너비와 중심을 유지한다.
- 창 등록은 실제 View attachment보다 먼저 완료된다. 등록한 창을 명시적으로 추적하며, 회전·반전·표시 변경으로 메뉴 창을 제거하거나 다시 생성하지 않는다. 화면 크기 변경에도 펼친 메뉴와 팝업을 유지한다.
- 삼성 카메라가 세로 창을 유지하면서 버튼만 회전할 때는 OrientationEventListener로 로고·액션 아이콘을 회전시킨다. 가로 메뉴는 상단의 기본 버튼 전 빈 공간에 아이콘만 가로로 표시하며, 실제 가로 창에서 측면 여백이 충분하면 그 영역에 세로로 배치한다. FULL은 기본 카메라 버튼과 같은 상단 영역을 사용한다.
- 그리드는 실제 display rotation의 역변환을 적용한다. 센서에 따른 버튼 회전은 그리드에 적용하지 않는다. 사용자가 선택한 회전과 반전은 물리 좌표에서 유지하며, 미리보기 카드는 display 보상을 적용하지 않는다.
- 황금 나선은 황금 직사각형의 테두리, 오른쪽→아래→왼쪽→위의 정사각형 분할선, 연결된 사분원 원호다. 동적 대칭의 보조 수선은 모두 프레임 경계까지 연장한다.
- 선택 테두리와 그리드 미리보기 선은 밝은 테마에서 검정, 어두운 테마에서 흰색이다. 카메라 라인의 사용자 색상은 미리보기 색상에 영향을 주지 않는다.
- 라인 스타일 초기화 아이콘 한 개가 불투명도 90%와 두께 1.2dp를 함께 복원한다. 슬라이더 thumb는 테마 surface와 파란 ring을 사용한다. 검은 테두리를 추가하지 않는다.
- 앱 버튼의 기본 StateListAnimator 그림자는 제거한다. 하단 탐색은 배경 블러와 반투명 캡슐을 유지하고, 불투명 outline을 통한 elevation 그림자를 표시한다.
- 백그라운드 실행 끄기와 접근성 끄기는 구분선이 있는 일반 텍스트 액션 행이다. 접근성 행은 One UI의 공개 접근성 설정을 연다. 설치된 앱 → Chrynalogia에서 끈다. 상세 화면 직행 인텐트는 One UI 8.5의 시스템 전용 권한 때문에 사용하지 않는다.
- 언어 행은 Android `ACTION_APP_LOCALE_SETTINGS`로 해당 앱의 One UI 언어 설정을 연다. 앱 내부 언어 선택 팝업을 제공하지 않는다. 이 화면에서 돌아와도 종료한 수동 세션을 시작하지 않는다.
- 카메라 팝업은 시스템 테마가 바뀔 때 창을 유지하면서 내용의 테마를 갱신한다. 선택 탭과 스크롤을 복원한다. KitSwitch는 체크 변경 직후 직접 invalidate한다.

추가 원본 Icons 페이지 SVG: `perspective_horizontal` (`340:8724`), `perspective_vertical` (`340:8723`), `filter_tab` (`340:8359`), `refresh`. 모두 Figma의 일반 SVG 내보내기로 받은 원본이다. 기존 flip/color_adjustment 대체 리소스는 제거했다.

## v0.14 overrides

- 삼성 카메라의 창과 미리보기 좌표가 유지되면 휴대폰 방향이 바뀌어도 로고 창·메뉴·설정 팝업의 위치와 배치를 유지한다. 센서는 아이콘 내용만 회전시킨다. 실제 카메라 창/미리보기의 측정 좌표가 바뀔 때만 다시 배치한다. v0.13의 센서 기반 가로 메뉴 전환을 대체한다.
- 카메라 UI가 잠깐 없을 때는 이전에 확인한 동일한 window ID만 최대 750ms 유지한다. 다른 앱·시스템 창·잠금 화면은 즉시 숨긴다. 자체 설정 팝업의 root로 카메라 위치를 추측하지 않는다.
- 좌우 반전에는 `perspective_vertical`, 상하 반전에는 `perspective_horizontal` 원본 SVG를 사용한다. 눈 아이콘은 SVG를 수정하지 않고 투명 여백을 제외한 실제 도형을 같은 20dp 영역에 맞춘다.
- 테마 팝업은 28dp 둥근 컨테이너, 테마별 container/text 색상, 24sp 제목, 18sp 라디오 선택 행, 파란 닫기 액션으로 직접 구성한다. 기본 플랫폼 AlertDialog의 제목·선택 목록 스타일에 의존하지 않는다.
- ‘앱 백그라운드 실행’은 실행 상태 스위치다. 18sp 제목 아래 14sp 상태 설명을 두고 실행 중은 파랑, 꺼짐은 보조 회색이다. 위아래 12dp 간격을 유지하고 서비스 준비/종료 직후 실제 상태를 갱신한다. 접근성 이동은 구분선 일반 액션 행을 유지한다.
- 접근성 행은 공개 One UI 설정을 열고, 연결된 서비스로 ‘설치된 앱 → Chrynalogia → 사용 중’의 다음 항목을 강조한다. 안내는 터치를 가로채지 않으며 설정 밖으로 이동하거나 90초가 지나면 제거한다. 권한을 자동 변경하지 않는다. 서비스가 이미 꺼져 있으면 경로 안내 문구를 표시한다. 상세 화면의 시스템 전용 권한은 추가하지 않는다.

## v0.15 overrides

- 사용자 요청에 따라 접근성 안내 오버레이와 안내 문구를 제거한다. 접근성 행은 공개 One UI 설정만 연다. v0.14의 강조 안내는 더 이상 제공하지 않는다.
- 카메라 버튼의 기준 위치는 휴대폰의 기본 방향 좌표에 보관한다. 삼성 버튼의 화면 좌표도 이 기준으로 변환한 뒤 여백을 읽는다. 실제 display 회전에는 로고·그룹 메뉴·팝업의 사각형을 함께 투영한다. 세로로 돌아오면 동일한 기준 위치를 복원한다. 촬영 버튼 영역으로 메뉴를 다시 배치하지 않는다.
- display가 회전하면 논리 좌표의 메뉴 행/열과 순서가 바뀌지만, 휴대폰에서 차지하는 실제 위치는 유지한다. 기존 창은 유지하고 셀 순서만 조정한다. 센서만 바뀔 때는 내용의 각도만 바꾼다.
- 센서의 시계 방향과 display의 반대 방향 정의를 함께 보정해 가로 방향에서 아이콘이 뒤집히지 않도록 한다. 기준 위치를 만들 때 잘못된 방향의 오래된 삼성 UI 노드는 제외하고 확인한 여백을 유지한다.
- 실제 회전 검사는 `wm fixed-to-user-rotation`을 테스트 동안만 켜 양쪽 가로·180도·세로 복귀를 실행한다. 아이콘용 디버그 입력만 바꾸는 검사는 실제 display 회전 검사를 대체하지 않는다. 원래 회전 설정은 검사 후 복원한다.

## v0.16 overrides

- 새 설치의 첫 실행에 One UI 시스템 버전 feature가 없으면 삼성 카메라 전용이며 현재 기기에서 사용할 수 없다는 안내를 표시한다. 제조사 이름으로 OS를 추정하지 않는다.
- 안내는 기존 테마 팝업과 같은 One UI 색상, 28dp 모서리, 24sp 제목, 18sp 본문, 파란 ‘계속 사용’ 액션을 사용한다. 앱 언어와 테마를 따른다.
- 사용자가 안내를 닫으면 확인 여부를 저장한다. 회전으로 Activity가 종료된 경우에는 확인으로 처리하지 않는다. 지원 기기와 이미 초기 설정을 마친 설치에는 표시하지 않는다. 특수 환경을 위해 어떤 기능도 차단하지 않는다.

## v0.17 overrides

- 카메라 메뉴의 좌우·상하 반전은 현재 휴대폰을 잡고 있는 방향을 기준으로 적용한다. 가로 방향에서는 저장된 기본 좌표의 반전 축을 교환한다. 기존 그리드의 물리 방향 보존과 사용자가 지정한 회전은 유지한다.
- 방향 전환 중 로고·메뉴·열린 설정의 기존 배치를 유지한다. 아이콘만 즉시 회전하며, 실제 display·기본 방향 창·미리보기 좌표가 250ms 동안 일치한 뒤 배치를 갱신한다. 일반 비율 변경에는 이 지연을 적용하지 않는다.
- 노드가 없거나 기존 미리보기를 임시 보존한 상태는 새 배치의 측정값으로 인정하지 않는다. 노드가 돌아온 뒤 안정화 시간을 다시 센다. 팝업 닫기는 대기 갱신을 취소하지 않으며 카메라 종료는 취소한다.

## v0.18 overrides

- 사용자 요청에 따라 v0.17의 좌표 안정화 대기·재시도 타이머를 제거한다. 기존 v0.16처럼 측정 좌표와 실제 display 변경에 바로 배치를 갱신한다. 가로 방향 반전 축 수정은 유지한다.
