package com.voxinsight.service;

import com.voxinsight.dto.HelpResponse;
import com.voxinsight.dto.HelpResponse.About;
import com.voxinsight.dto.HelpResponse.Article;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.MetaRow;
import com.voxinsight.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HelpService {

  private final TrainingMapper trainingMapper;

  public HelpService(TrainingMapper trainingMapper) {
    this.trainingMapper = trainingMapper;
  }

  public HelpResponse load() {
    MetaRow meta = trainingMapper.findMeta();
    if (meta == null) {
      throw new NotFoundException("没有找到应用信息。");
    }
    List<Article> articles =
        trainingMapper.listArticles().stream()
            .map(row -> new Article(row.title, row.content))
            .toList();
    return new HelpResponse(
        articles,
        new About(meta.brandNameZh, meta.brandNameEn, meta.slogan, meta.aboutText, meta.versionName));
  }
}
