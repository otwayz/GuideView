# GuideView

实现 引导蒙层

蒙层效果不拘泥于 TargetView 的方位。

实现原理：

添加一个 引导层 的 layout
layout 中需要用一个View 代替 需要引导的 View 。并且两者 id 需要统一。

在获取到 TargetView 尺寸参数后。 将蒙层布局中的 View 更新尺寸 位置。

在onDraw 中画出 镂空区域

使用方式

```
  guideView = GuideView.Builder.newInstance(this)
                    .setCustomView(R.layout.layout_guide) //蒙层中的占位View id 同
                    .setTargetView(findViewById(R.id.id_tv_hello))
                    .setOnClickListener(R.id.id_btn_close, {
                        guideView.hide()
                    })
                    .build()

  guideView.show()
```


![View](https://github.com/otwayz/GuideView/blob/master/image/guide.png)
