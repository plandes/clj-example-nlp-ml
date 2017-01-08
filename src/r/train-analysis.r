overfit.graph <- function(analysis.dir, datname, export) {
    fname <- sprintf('%s/%s.csv', analysis.dir, datname)
    pdfname <- sprintf('%s/%s.pdf', analysis.dir, datname)
    testdf <- read.csv(fname, header=TRUE)
    y <- testdf[,'F.Measure']
    dps <- length(y)
    x <- testdf[,'Train']
    incy <- sapply(1:dps, function(i) {all(y[1:i] == cummax(y[1:i]))})
    elbow <- match(c(FALSE), incy)
    if (export) pdf(pdfname)
    par(new=FALSE)
    plot(x, y, ylab='F-Measure', xlab='Training Instances')
    lser <- y~x
    lines(lser, col='blue', lwd=1)
    eblser <- y[(elbow-1):elbow]~x[(elbow-1):elbow]
    lines(eblser, col='red', lwd=3)
    text(x, y, labels=x, cex=0.7, adj=c(0.5, -1), col="black")
    par(new=TRUE)
    if (export) {
        print(sprintf('writing pdf file: %s', pdfname))
        dev.off()
    }
}

#adir <- sprintf('%s/Desktop', Sys.getenv('HOME'))
adir <- '../../doc/results'
dname <- 'speech-act-J48-train-test-series'
overfit.graph(adir, dname, TRUE)
