GitHub Wiki 初始化步骤（仅需一次网页操作）：
1. 浏览器打开 https://github.com/materin-tech/materin-ops/wiki
2. 点击 "Create the first page"，随便输入一个字符保存
3. 之后本目录即可作为 wiki 仓库推送：
   git clone https://github.com/materin-tech/materin-ops.wiki.git
   cp wiki-pages/*.md <wiki克隆目录>/
   cd <wiki克隆目录> && git add . && git commit -m "docs: wiki 页面" && git push
（Home.md 为导航首页，部署指南.md 为完整部署文档）
