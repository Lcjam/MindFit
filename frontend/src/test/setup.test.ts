describe('Test environment setup', () => {
  it('jest-dom matchers are available', () => {
    const el = document.createElement('div')
    el.textContent = 'hello'
    document.body.appendChild(el)
    expect(el).toBeInTheDocument()
  })
})
