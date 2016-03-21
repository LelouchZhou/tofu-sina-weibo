新浪微博android客户端(一个用于Android手机的开源新浪微博客户端)，部分功能介绍：
1.OAuth1.0认证
2.支持下拉刷新，自动加载更多的ListView--AutoGetMoreListView.java
3.图片异步下载，同时可设置在加载图片时候显示的图片或多张图片顺序播放--AsyncImageView.java
4.支持GIF动态图查看，多点触控浏览图片。--MyImageView.java && MyView.java && GIFDecode.java && GIFView.java
5.文字超链接，微博人名，@ ，网址高亮且添加超链接，其中点击人名自动跳转到查看用户信息，话题高亮。全部都是用正则匹配。解决了ListView上TextView使用Linkify导致点击不响应OnItemClickListener的情况--HighLightTextView.java
6.纸微博，把文字写在图片上。其中为了对齐，全部符号装成全角形式。--ImageRel.java
7.抽屉式显示评论（类似口袋微博，不过界面不可同日而语）。--ViewActivity.java
8.桌面挂件App widget显示动画（滚动显示微博）。--AppWidgetAnimationService.java && WeiboAppWidget.java
9.TextView跟EditText显示表情，其中EditText还实时显示表情，一匹配即显示，同时还有九宫格显示表情列表。 --HighLightTextView.java && ShareActivity.java
10.当点击TabHost中的TabWidget，假如当前显示的跟点击的一样，显示listView第一个item，否则切换TabWidget.
11.全部下载通过新开线程实现。
12.具有分享功能。

ps1:代码风格不是很良好。
ps2:寒假会更新。