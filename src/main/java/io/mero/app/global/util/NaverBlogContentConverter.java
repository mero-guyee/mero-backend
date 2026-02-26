package io.mero.app.global.util;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.FootprintLocation;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Component
public class NaverBlogContentConverter {

    /**
     * Footprint를 네이버 블로그 게시용 HTML로 변환
     */
    public String convert(Footprint footprint) {
        StringBuilder html = new StringBuilder();

        // 날짜 헤더
        html.append("<h2>")
                .append(HtmlUtils.htmlEscape(footprint.getDate().toString()))
                .append("</h2>");

        // 날씨 정보
        if (footprint.getWeatherInfo() != null && !footprint.getWeatherInfo().isBlank()) {
            html.append("<p>")
                    .append(HtmlUtils.htmlEscape(footprint.getWeatherInfo()))
                    .append("</p>");
        }

        // 위치 목록
        List<FootprintLocation> locations = footprint.getLocations();
        if (!locations.isEmpty()) {
            html.append("<ul>");
            for (FootprintLocation loc : locations) {
                html.append("<li>📍 ")
                        .append(HtmlUtils.htmlEscape(loc.getPlaceName()));
                if (loc.getCity() != null && !loc.getCity().isBlank()) {
                    html.append(", ").append(HtmlUtils.htmlEscape(loc.getCity()));
                }
                if (loc.getCountry() != null && !loc.getCountry().isBlank()) {
                    html.append(", ").append(HtmlUtils.htmlEscape(loc.getCountry()));
                }
                html.append("</li>");
            }
            html.append("</ul>");
        }

        // 본문 내용
        if (footprint.getContent() != null && !footprint.getContent().isBlank()) {
            String[] paragraphs = footprint.getContent().split("\n");
            for (String paragraph : paragraphs) {
                if (!paragraph.isBlank()) {
                    html.append("<p>")
                            .append(HtmlUtils.htmlEscape(paragraph))
                            .append("</p>");
                }
            }
        }

        // 사진
        List<String> photoUrls = footprint.getPhotoUrls();
        if (!photoUrls.isEmpty()) {
            for (String url : photoUrls) {
                html.append("<img src=\"")
                        .append(HtmlUtils.htmlEscape(url))
                        .append("\" style=\"max-width:100%;\" />");
            }
        }

        return html.toString();
    }
}
